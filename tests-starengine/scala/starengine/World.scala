package starengine

import ProxyBwapi.Races.Protoss

/**
 * The StarEngine world: two sides, one frame at a time. Owns the loop
 * only - pilots decide, constraints adjust, and this object moves bodies
 * and resolves shots. Nearest-with-predicate is a query, not a decision:
 * pilots and the gun resolution share it instead of each copying it.
 *
 * Order per frame (documented, fixed): pilots decide, due storms land
 * on the positions where the previous frame ended, then ours move,
 * then foes, then constraints adjust, then casts commit, then foes
 * shoot, then ours shoot. Foes shoot first so outcomes never depend
 * on who the engine favors. A committed storm lands StormCastFrames
 * later from its committed point: casting takes time, so pilots lead
 * moving victims - the landing frame sees exactly castFrames of
 * drift, which is what the lead covers. Casting never counts as a
 * reissue and never moves the caster: storming does not break march
 * continuity the way redirecting does.
 */
final class World(
  val ours: Seq[Body],
  val foes: Seq[Body],
  val pilots: Map[Body, Pilot],
  val constraints: Seq[Constraint]) {

  var frame: Int = 0

  /** Reissues per body: every order change costs the frame (re-aiming resets acceleration to a crawl). */
  val reissues = new scala.collection.mutable.HashMap[Body, Int]().withDefaultValue(0)
  private val lastOrder = new scala.collection.mutable.HashMap[Body, Directive]()

  /** Storms cast per body, in order. */
  val stormsCast = new scala.collection.mutable.HashMap[Body, Int]().withDefaultValue(0)

  /**
   * Blocked rectangles, in pixels (x1, y1, x2, y2, any corner order):
   * unwalkable ground - seals, walls, map edges the scenario models.
   * Empty by default, so scenarios without terrain read exactly as before.
   */
  var blockedRects: Seq[(Double, Double, Double, Double)] = Seq.empty

  /**
   * Straight-line ground path (mirrors groundPathExists, approximated):
   * true unless the segment crosses a blocked rect. The engine walks
   * straight lines, so a blocked segment reads as no path - no
   * triangulation is modeled, and scenarios document the rects they assume.
   */
  def groundPathExists(ax: Double, ay: Double, bx: Double, by: Double): Boolean =
    blockedRects.forall { case (x1, y1, x2, y2) => ! segmentHitsRect(ax, ay, bx, by, x1, y1, x2, y2) }

  private def segmentHitsRect(
    ax: Double, ay: Double, bx: Double, by: Double,
    x1: Double, y1: Double, x2: Double, y2: Double): Boolean = {
    val (lox, hix) = if (x1 < x2) (x1, x2) else (x2, x1)
    val (loy, hiy) = if (y1 < y2) (y1, y2) else (y2, y1)
    // Endpoint inside reads as blocked: sealed in means reforming inside.
    if (ax >= lox && ax <= hix && ay >= loy && ay <= hiy) return true

    if (bx >= lox && bx <= hix && by >= loy && by <= hiy) return true

    def cross(px: Double, py: Double, qx: Double, qy: Double, rx: Double, ry: Double): Double =
      (qx - px) * (ry - py) - (qy - py) * (rx - px)
    val edges = Seq(
      (lox, loy, hix, loy), (hix, loy, hix, hiy),
      (hix, hiy, lox, hiy), (lox, hiy, lox, loy))
    edges.exists { case (ex1, ey1, ex2, ey2) =>
      val d1 = cross(ex1, ey1, ex2, ey2, ax, ay)
      val d2 = cross(ex1, ey1, ex2, ey2, bx, by)
      val d3 = cross(ax, ay, bx, by, ex1, ey1)
      val d4 = cross(ax, ay, bx, by, ex2, ey2)
      ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
    }
  }

  /** Attack lock, in frames: a body that just dealt damage holds still (no re-aiming mid-swing). */
  val attackLock = new scala.collection.mutable.HashMap[Body, Int]().withDefaultValue(0)

  /**
   * Committed pick, last frame: what a body was ordered to do. Pilots
   * read each other's intents through this the way production reads
   * presumptive targets - a foe chasing one of ours is closing
   * regardless of whether the distance trend shows it yet.
   */
  def orderOf(body: Body): Option[Directive] = lastOrder.get(body)

  /**
   * Bugged reaver launch (scenario opt-in, off by default): scarabs
   * against defensive statics only leave inside the launch band and
   * only from a hold - outside it, or on the move, the hull stands
   * ready forever without firing. The BW behavior the Sunkens2 map
   * dies on: engine range claims the shot, the game never launches
   * it. Scenarios without the concept read exactly as before.
   */
  var buggedStaticLaunch: Boolean = false

  /** Committed storms waiting to land: (landing frame, x, y). */
  private val pendingStorms = new scala.collection.mutable.ListBuffer[(Int, Double, Double)]()

  /** Same order if same target, a target switch inside 32 pixels (dithering, not redirecting), or a goal that barely moved (waypoint following). */
  private def sameOrderAs(previous: Option[Directive], next: Directive): Boolean = (previous, next) match {
    case (None, _) => true
    case (Some(Hold), Hold) => true
    case (Some(Chase(a)), Chase(c)) => (a eq c) || a.dist(c) <= 32.0
    case (Some(Board(a)), Board(b)) => a eq b
    case (Some(Unload), Unload) => true
    case (Some(CastStorm(x1, y1)), CastStorm(x2, y2)) =>
      val dx = x1 - x2
      val dy = y1 - y2
      dx * dx + dy * dy <= 32.0 * 32.0
    case (Some(MoveTo(x1, y1)), MoveTo(x2, y2)) =>
      val dx = x1 - x2
      val dy = y1 - y2
      dx * dx + dy * dy <= 32.0 * 32.0
    case _ => false
  }

  def nearestFoeOf(me: Body, among: Seq[Body], valid: (Body, Body) => Boolean): Option[Body] = {
    val live = among.view.filter(b => b.alive && b.transport.isEmpty && valid(me, b))
    if (live.isEmpty) None else Some(live.minBy(me.dist))
  }

  /**
   * Ground-truth detection (mirrors tile.enemyDetected, not knowledge):
   * live foe detectors whose sight reaches the body. Pilots use this
   * ONLY for the revealed flag - hunting and evading steer on what the
   * body itself sees, which is exactly the gap the sight-disadvantage
   * scenarios isolate (an Observer outranges the Templar, so the tile
   * burns before the eye is known).
   */
  def detectorsRevealing(me: Body): Seq[Body] = {
    val foesOf = if (ours.contains(me)) foes else ours
    foesOf.filter(f => f.alive && f.kind.isDetector && me.dist(f) < f.kind.sightPixels)
  }

  /**
   * Under-attack flag (mirrors isUnderAttack, windowed): true while
   * damage landed within the sticky window - BWAPI's flag outlives the
   * exact damage frame too, so a single pooled window stands in for it.
   */
  def underAttack(me: Body): Boolean =
    me.lastDamageFrame >= 0 && frame - me.lastDamageFrame <= World.UnderAttackWindowFrames

  /** Cargo slots taken aboard a transport, in BW slots. */
  def cargoSpaceUsed(transport: Body): Int =
    (ours ++ foes).view.filter(b => b.alive && b.transport.contains(transport)).map(_.kind.spaceRequired).sum

  def step(): Unit = {
    val orders = (ours ++ foes).view.filter(_.alive)
      .map(b => b -> pilots.get(b).map(_.direct(b, this)).getOrElse(Hold)).toMap
    landStorms()
    moveSide(ours, orders)
    moveSide(foes, orders)
    syncCargo()
    constraints.foreach(_.apply(this))
    castSide(ours, orders)
    shootSide(foes, ours, orders)
    shootSide(ours, foes, orders)
    frame += 1
  }

  def run(frames: Int): Unit = {
    var remaining = frames
    while (remaining > 0 && ours.exists(_.alive)) {
      step()
      remaining -= 1
    }
  }

  private def moveSide(side: Seq[Body], orders: Map[Body, Directive]): Unit = {
    side.filter(_.alive).foreach(body => {
      if (attackLock.getOrElse(body, 0) > 0) {
        // Rooted mid-swing: the order stands, the body does not move, and
        // resuming it later is no redirect.
        attackLock(body) = attackLock(body) - 1
      } else {
        val order = orders.getOrElse(body, Hold)
        // Casts resolve in the cast phase from post-move positions: no move,
        // no reissue, and the march memory is untouched so storming never
        // reads as dithering.
        if ( ! order.isInstanceOf[CastStorm]) {
          if (sameOrderAs(lastOrder.get(body), order)) {
            moveBody(body, order)
          } else {
            reissues(body) = reissues(body) + 1
          }
          lastOrder.put(body, order)
        }
      }
    })
  }

  private def moveBody(body: Body, order: Directive): Unit = {
    order match {
        // Casts resolve in the cast phase from post-move positions, never here.
        case CastStorm(_, _) => ()
        case Unload =>
          body.transport = None
        case _ if body.transport.isDefined =>
          // Loaded cargo rides: boarding twice is a no-op, other orders
          // wait for the disembark - the sync pass moves the hull.
          ()
        case Board(t) =>
          if (t.alive && t.transport.isEmpty
            && cargoSpaceUsed(t) + body.kind.spaceRequired <= t.kind.spaceProvided
            && body.dist(t) <= World.BoardRangePx) {
            body.transport = Some(t)
          } else if (t.alive) {
            body.stepToward(t.x, t.y)
          }
        case Hold => ()
        case MoveTo(x, y) => body.stepToward(x, y)
        case Chase(target) =>
          if (target.alive) {
            // Statics stop the stride at reach (no jumping the guns);
            // mobiles are chased full-speed, or a capped stride lets
            // anything faster hold the rim forever.
            val stopEdge = if (target.speed == 0) body.rangeAgainst(target) else 0.0
            val dist = body.dist(target)
            if (dist > stopEdge) {
              val stride = Math.min(body.speed, dist - stopEdge)
              val dx = target.x - body.x
              val dy = target.y - body.y
              body.x += dx / dist * stride
              body.y += dy / dist * stride
            }
          }
      }
  }

  /**
   * Loaded cargo rides its transport: after every move, each boarded
   * body snaps to its hull. Transports moved this same frame already,
   * so one sync after both sides keeps the pair pixel-exact. Cargo goes
   * down with a dying hull (BW-faithful: no bailing out of a kill).
   */
  private def syncCargo(): Unit = {
    (ours ++ foes).filter(_.alive).foreach(cargo =>
      cargo.transport.foreach(hull => {
        if (hull.alive) { cargo.x = hull.x; cargo.y = hull.y }
        else { cargo.hp = 0; cargo.transport = None }
      }))
  }

  /**
   * Storm casts, ours only (no foe caster exists in these scenarios).
   * A cast deducts the battery and lands StormCastFrames later - the
   * game cast takes time, so the pilot must lead moving victims. Casts
   * land from their committed point no matter what the caster does next;
   * landing deals flat damage to every body in the box - friend or foe,
   * the game does not discriminate either. Casters do not shoot in the
   * cast frame; the orders map carries who cast into the gun phase.
   */
  private def castSide(side: Seq[Body], orders: Map[Body, Directive]): Unit = {
    side.filter(b => b.alive && b.transport.isEmpty).foreach(body => {
      orders.get(body).foreach {
        case CastStorm(x, y) if body.energy >= World.StormCostEnergy =>
          body.energy -= World.StormCostEnergy
          stormsCast(body) = stormsCast(body) + 1
          pendingStorms.append((frame + World.StormCastFrames, x, y))
        case _ => ()
      }
    })
  }

  /**
   * Bugged static launch, one home: a Reaver shelling a defensive
   * static only resolves inside the launch band and only from a hold
   * order - the band is the shipped hold kernel's number, pinned equal
   * by suite so the bug model and the fix can never drift apart.
   * Everything else (mobiles, other shooters, the flag off) resolves
   * exactly as before.
   */
  private def staticLaunchResolves(me: Body, foe: Body, orders: Map[Body, Directive]): Boolean =
    ! buggedStaticLaunch ||
      me.kind != Protoss.Reaver ||
      ! foe.kind.isStaticDefense ||
      (me.dist(foe) <= World.ReaverStaticLaunchBandPx &&
        orders.get(me).exists(_ == Hold))

  /** Storms due this frame land on post-move positions. Loaded cargo rides through: guns cannot hit it, storms neither. */
  private def landStorms(): Unit = {
    val due = pendingStorms.filter(_._1 <= frame)
    pendingStorms --= due
    due.foreach { case (_, x, y) =>
      (ours ++ foes).filter(b => b.alive && b.transport.isEmpty).foreach(victim => {
        if (Math.abs(victim.x - x) <= World.StormBoxHalfPx && Math.abs(victim.y - y) <= World.StormBoxHalfPx) {
          victim.hp = Math.max(0, victim.hp - World.StormDamage)
        }
      })
    }
  }

  private def shootSide(side: Seq[Body], victims: Seq[Body], orders: Map[Body, Directive]): Unit = {
    side.filter(b => b.alive && ! b.passive).foreach(body => {
      if (body.cooldownLeft > 0) {
        body.cooldownLeft -= 1
      } else if (body.transport.isDefined) {
        // Loaded cargo rides: no guns from inside the hull.
      } else if (orders.get(body).exists(_.isInstanceOf[CastStorm])) {
        // Cast this frame: the energy is already spent, the guns stay silent.
      } else {
        // The chase target is the committed pick (mirrors toAttack): shoot
        // it while it holds, otherwise fall back to the nearest hittable.
        // Loaded bodies are not on the field: neither committed nor found.
        // Cloaked bodies are not on the field either without detection
        // (mirrors cloak + detector coverage): guns skip them unless a
        // live foe detector's sight reaches them. Nothing is cloaked by
        // default, so scenarios without the concept read exactly as before.
        val committed = orders.get(body).collect {
          case Chase(target) if target.alive && target.transport.isEmpty && body.canHit(target) && body.dist(target) <= body.rangeAgainst(target) && staticLaunchResolves(body, target, orders) && ( ! target.cloaked || detectorsRevealing(target).nonEmpty) => target
        }
        val victim = committed.orElse(nearestFoeOf(body, victims, (me, foe) => me.canHit(foe) && me.dist(foe) <= me.rangeAgainst(foe) && staticLaunchResolves(me, foe, orders) && ( ! foe.cloaked || detectorsRevealing(foe).nonEmpty)))
        victim.foreach(v => {
          v.hp = Math.max(0, v.hp - body.damageAgainst(v))
          v.lastDamageFrame = frame
          // Radial splash approximation (no divisors, no scarab flight):
          // the primary takes the full shot, every other live enemy in
          // the outer radius takes half. Scarabs are enemy-splash, so
          // our own bodies never catch it. Loaded cargo never catches it.
          if (body.splashOuterPx > 0) {
            victims.filter(o => o.alive && o.transport.isEmpty && (o ne v) && o.dist(v) <= body.splashOuterPx).foreach(o => {
              o.hp = Math.max(0, o.hp - body.damageAgainst(o) / 2)
              o.lastDamageFrame = frame
            })
          }
          body.cooldownLeft = body.cooldownAgainst(v)
        })
      }
    })
  }
}

/**
 * Storm numbers, one home (SRP): the cast phase and the pilots read
 * these instead of each copying them. Cost is the game cost
 * (TechType.Psionic_Storm.energyCost); damage is the full-game total
 * dealt instantly (no lingering field in this engine); the box matches
 * the 96px search box the shipped targeting uses; the cast frames mirror
 * the shipped PsionicStorm.castFrames (iScript-derived, observed by
 * McRave - the closest thing BWAPI has to a cast time), pinned equal
 * by suite so the two can never drift apart.
 */
object World {

  /**
   * Reaver launch band, in pixels: scarabs vs defensive statics only
   * resolve inside this, and only from a hold. Mirrors the shipped
   * ReaverStaticHold band - pinned equal by suite, never from memory.
   */
  val ReaverStaticLaunchBandPx: Double = 7.25 * 32.0

  val StormCostEnergy: Int = 75
  val StormDamage: Int = 112
  val StormBoxHalfPx: Double = 48.0
  val StormCastRangePx: Double = 9 * 32.0
  val StormCastFrames: Int = 4

  /**
   * Pickup reach, in pixels: boarding needs the hulls side by side,
   * the engine's point-body stand-in for the 32px edge-to-edge pickup
   * radius the shipped reboard rule quotes.
   */
  val BoardRangePx: Double = 32.0

  /**
   * Sticky under-attack window, in frames: damage inside the window
   * reads as under attack, the stand-in for BWAPI's isUnderAttack flag
   * outliving the exact damage frame. Short enough that a duel gone
   * quiet re-arms the hunt, long enough to bridge one cooldown between
   * volleys so incoming fire keeps the withdraw standing.
   */
  val UnderAttackWindowFrames: Int = 12
}
