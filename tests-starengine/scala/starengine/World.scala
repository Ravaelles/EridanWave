package starengine

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

  /** Committed storms waiting to land: (landing frame, x, y). */
  private val pendingStorms = new scala.collection.mutable.ListBuffer[(Int, Double, Double)]()

  /** Same order if same target, or a goal that barely moved (waypoint following, not redirecting). */
  private def sameOrderAs(previous: Option[Directive], next: Directive): Boolean = (previous, next) match {
    case (None, _) => true
    case (Some(Hold), Hold) => true
    case (Some(Chase(a)), Chase(c)) => a eq c
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
    val live = among.view.filter(b => b.alive && valid(me, b))
    if (live.isEmpty) None else Some(live.minBy(me.dist))
  }

  def step(): Unit = {
    val orders = (ours ++ foes).view.filter(_.alive)
      .map(b => b -> pilots.get(b).map(_.direct(b, this)).getOrElse(Hold)).toMap
    landStorms()
    moveSide(ours, orders)
    moveSide(foes, orders)
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
    })
  }

  private def moveBody(body: Body, order: Directive): Unit = {
    order match {
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
   * Storm casts, ours only (no foe caster exists in these scenarios).
   * A cast deducts the battery and lands StormCastFrames later - the
   * game cast takes time, so the pilot must lead moving victims. Casts
   * land from their committed point no matter what the caster does next;
   * landing deals flat damage to every body in the box - friend or foe,
   * the game does not discriminate either. Casters do not shoot in the
   * cast frame; the orders map carries who cast into the gun phase.
   */
  private def castSide(side: Seq[Body], orders: Map[Body, Directive]): Unit = {
    side.filter(_.alive).foreach(body => {
      orders.get(body).foreach {
        case CastStorm(x, y) if body.energy >= World.StormCostEnergy =>
          body.energy -= World.StormCostEnergy
          stormsCast(body) = stormsCast(body) + 1
          pendingStorms.append((frame + World.StormCastFrames, x, y))
        case _ => ()
      }
    })
  }

  /** Storms due this frame land on post-move positions. */
  private def landStorms(): Unit = {
    val due = pendingStorms.filter(_._1 <= frame)
    pendingStorms --= due
    due.foreach { case (_, x, y) =>
      (ours ++ foes).filter(_.alive).foreach(victim => {
        if (Math.abs(victim.x - x) <= World.StormBoxHalfPx && Math.abs(victim.y - y) <= World.StormBoxHalfPx) {
          victim.hp = Math.max(0, victim.hp - World.StormDamage)
        }
      })
    }
  }

  private def shootSide(side: Seq[Body], victims: Seq[Body], orders: Map[Body, Directive]): Unit = {
    side.filter(_.alive).foreach(body => {
      if (body.cooldownLeft > 0) {
        body.cooldownLeft -= 1
      } else if (orders.get(body).exists(_.isInstanceOf[CastStorm])) {
        // Cast this frame: the energy is already spent, the guns stay silent.
      } else {
        // The chase target is the committed pick (mirrors toAttack): shoot
        // it while it holds, otherwise fall back to the nearest hittable.
        val committed = orders.get(body).collect {
          case Chase(target) if target.alive && body.canHit(target) && body.dist(target) <= body.rangeAgainst(target) => target
        }
        val victim = committed.orElse(nearestFoeOf(body, victims, (me, foe) => me.canHit(foe) && me.dist(foe) <= me.rangeAgainst(foe)))
        victim.foreach(v => {
          v.hp = Math.max(0, v.hp - body.damageAgainst(v))
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

  val StormCostEnergy: Int = 75
  val StormDamage: Int = 112
  val StormBoxHalfPx: Double = 48.0
  val StormCastRangePx: Double = 9 * 32.0
  val StormCastFrames: Int = 4
}
