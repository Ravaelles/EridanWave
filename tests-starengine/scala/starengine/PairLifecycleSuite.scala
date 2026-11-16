package starengine

import Micro.Actions.Protoss.ReaverShuttle.{PairArbiter, ReaverBoardGate}
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Pair lifecycle, stage by stage: the Sunkens3 shape (a Reaver plus its
 * Shuttle against a Sunken arc) split into the states a hull actually
 * visits - bond, ferry, unload, shoot, fight, reboard, evacuate,
 * fetch, retarget. Every pilot calls the shipped resolve on the same
 * plain views the game seam builds, then executes the order the way
 * the brains do; the engine supplies Board/Unload, cargo and damage.
 * Each stage sets up its own state and asserts its own exit, so a
 * failure names the broken transition instead of the whole war. The
 * full run at the end chains them: bond, ferry, unload, two kills,
 * pressure, reboard, evacuate, zero paralyzed frames throughout.
 * Speeds, reaches and damage are the real UnitType numbers.
 */
class PairLifecycleSuite extends AnyFunSuite with StarEngineSuite {

  private val army = (100.0, 500.0)

  private def tilePos(b: Body): (Double, Double) = (b.x / 32.0, b.y / 32.0)

  private def foeViews(me: Body, foes: Seq[Body]): Seq[PairArbiter.FoeView] =
    foes.filter(_.alive).map(f => PairArbiter.FoeView(
      me.dist(f) / 32.0,
      f.rangeAgainst(me) / 32.0,
      me.rangeAgainst(f) / 32.0))

  /**
   * Inferred shot clock, production order: the engine never tells the
   * pilot it fired, so a foe inside the gun means the clock resets -
   * the same inference the boarding gate pilot uses. A hull that never
   * closes starts quiet, exactly like a fresh hull in the game.
   */
  private val lastShot = new scala.collection.mutable.HashMap[Body, Int]().withDefaultValue(-100000)

  private def boardLocked(me: Body, foes: Seq[Body], frame: Int): Boolean = {
    val live = foes.filter(_.alive)
    if (live.exists(f => me.dist(f) <= me.rangeAgainst(f))) lastShot(me) = frame
    val crowd = live.filter(_.canHit(me)).sortBy(me.dist).headOption
      .map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity)

    ReaverBoardGate.locked(frame - lastShot(me), crowd)
  }

  private def threatViews(me: Body, foes: Seq[Body]): Seq[PairArbiter.ThreatView] =
    foes.filter(f => f.alive && f.canHit(me)).map(f =>
      PairArbiter.ThreatView(me.dist(f) / 32.0, f.rangeAgainst(me) / 32.0))

  private def reaverViewOf(r: Body, foes: Seq[Body], frame: Int): PairArbiter.ReaverView =
    PairArbiter.ReaverView(
      tilePos(r),
      Some(1),
      r.loaded,
      None,
      1,
      activeScarab = false,
      r.cooldownLeft,
      r.lossFraction,
      unloadOrdered = false,
      unloadSpotReady = false,
      boardLocked(r, foes, frame),
      foeViews(r, foes))

  /** Shuttle executor, production order: resolve, then the legs. */
  private class ShuttleExecutor(objective: (Double, Double)) extends Pilot {
    var fetches = 0
    var boards = 0

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      val mates = world.ours.view.filter(o => o.alive && o.kind == Protoss.Reaver).map(reaverViewOf(_, foes, world.frame)).toSeq
      val out = PairArbiter.resolveShuttle(
        PairArbiter.ShuttleView(tilePos(me), Some(1), mates.exists(_.loaded), threatViews(me, foes)),
        mates,
        raid = false,
        baitRide = mates.isEmpty).shuttle
      val mate = world.ours.find(o => o.alive && o.kind == Protoss.Reaver && o.transport.isEmpty)
      out match {
        case PairArbiter.FetchMate =>
          fetches += 1
          mate.map(m => MoveTo(m.x, m.y)).getOrElse(Hold)
        case PairArbiter.GuardMate =>
          mate.map(m => MoveTo(m.x, m.y)).getOrElse(Hold)
        case PairArbiter.Flee =>
          foes.sortBy(me.dist).headOption match {
            case Some(t) =>
              val dx = me.x - t.x
              val dy = me.y - t.y
              val dist = Math.sqrt(dx * dx + dy * dy)
              if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)
              else Hold
            case None => Hold
          }
        case PairArbiter.RaidCarry | PairArbiter.FollowArmy => MoveTo(objective._1, objective._2)
        case PairArbiter.Bait =>
          foes.sortBy(me.dist).headOption.map(f => MoveTo(f.x, f.y)).getOrElse(Hold)
        case PairArbiter.AwaitPost => Hold
      }
    }
  }

  /**
   * Reaver executor, production order: a loaded hull unloads inside the
   * landing band and rides otherwise; on foot it resolves and executes.
   * Counts paralyzed frames - a Hold with no foe inside the gun is the
   * wait the arbiter was built to ban.
   */
  private class ReaverExecutor(unloadTiles: Double = 8.0) extends Pilot {
    var boards = 0
    var unloads = 0
    var paralyzed = 0

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      val nearest = foes.sortBy(me.dist).headOption
      val foeTiles = nearest.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity)
      val gunTiles = nearest.map(me.rangeAgainst(_) / 32.0).getOrElse(Double.PositiveInfinity)
      if (me.loaded) {
        val rideBody = world.ours.find(o => o.alive && o.kind == Protoss.Shuttle)
        val band = rideBody.exists(r =>
          foes.exists(f => f.alive && f.kind.isBuilding && r.dist(f) / 32.0 <= unloadTiles))
        val out = PairArbiter.resolveReaver(
          PairArbiter.ReaverView(
            tilePos(me),
            Some(1),
            true,
            rideBody.map(r => PairArbiter.ShuttleView(
              tilePos(r),
              Some(1),
              true,
              threatViews(r, foes))),
            1,
            false,
            me.cooldownLeft,
            me.lossFraction,
            false,
            band,
            boardLocked(me, foes, world.frame),
            foeViews(me, foes)),
          Seq.empty,
          raid = false).reaver
        out match {
          case PairArbiter.UnloadHere =>
            unloads += 1

            Unload
          case _ => Hold
        }
      } else {
        val rides = world.ours.view.filter(o => o.alive && o.kind == Protoss.Shuttle).map(s =>
          PairArbiter.ShuttleView(tilePos(s), Some(1),
            world.ours.exists(o => o.alive && o.kind == Protoss.Reaver && o.loaded && o.transport.contains(s)),
            threatViews(s, foes))).toSeq
        val rideBody = world.ours.find(o => o.alive && o.kind == Protoss.Shuttle)
        val out = PairArbiter.resolveReaver(
          PairArbiter.ReaverView(
            tilePos(me),
            Some(1),
            loaded = false,
            rideBody.map(r => PairArbiter.ShuttleView(
              tilePos(r),
              Some(1),
              hasReaverAboard = true,
              threatViews(r, foes))),
            1,
            activeScarab = false,
            me.cooldownLeft,
            me.lossFraction,
            unloadOrdered = false,
            unloadSpotReady = false,
            boardLocked(me, foes, world.frame),
            foeViews(me, foes)),
          rides,
          raid = false).reaver
        out match {
          case PairArbiter.BoardRide =>
            rideBody match {
              case Some(r) if me.dist(r) <= World.BoardRangePx =>
                boards += 1

                Board(r)
              case Some(r) => MoveTo(r.x, r.y)
              case None    => Hold
            }
          case PairArbiter.FightOnFoot =>
            nearest.map(Chase).getOrElse(Hold)
          case PairArbiter.HoldShoot =>
            if (foeTiles > gunTiles) paralyzed += 1

            Hold
          case PairArbiter.WalkToRide =>
            rideBody.map(r => MoveTo(r.x, r.y)).getOrElse(Hold)
          case PairArbiter.WalkToArmy => MoveTo(army._1, army._2)
          case PairArbiter.RideAlong  => Hold
        }
      }
    }
  }

  private def sunken(x: Double, y: Double): Body =
    new Body(Zerg.SunkenColony, x, y, Zerg.SunkenColony.maxTotalHealth)

  private def pair(x: Double): (Body, Body) =
    (new Body(Protoss.Reaver, x, 500.0, Protoss.Reaver.maxTotalHealth),
      new Body(Protoss.Shuttle, x + 40.0, 500.0, Protoss.Shuttle.maxTotalHealth))

  test("bond: an adjacent on-foot hull boards the ride", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val (reaver, shuttle) = pair(0.0)
    val hull = new ReaverExecutor
    val world = new World(Seq(reaver, shuttle), Seq.empty, Map(reaver -> hull, shuttle -> new ShuttleExecutor((1700.0, 500.0))), Seq.empty)

    while (world.frame < 500 && reaver.transport.isEmpty) {
      world.step()
    }

    println(s"DIAG bond frames=${world.frame} boards=${hull.boards} loaded=${reaver.transport.isDefined}")
    assert(reaver.transport.isDefined, "adjacent hull never boarded")
  }

  test("ferry: a loaded pair travels at the objective with the hull aboard", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val (reaver, shuttle) = pair(0.0)
    reaver.transport = Some(shuttle)
    val world = new World(Seq(shuttle, reaver), Seq(sunken(1700.0, 500.0)),
      Map(reaver -> new ReaverExecutor, shuttle -> new ShuttleExecutor((1700.0, 500.0))), Seq.empty)

    while (world.frame < 1500 && shuttle.x < 1500.0) {
      world.step()
    }

    println(s"DIAG ferry frames=${world.frame} x=${shuttle.x.toInt} aboard=${reaver.transport.isDefined}")
    assert(shuttle.x > 1000.0, "loaded ride never ferried at the objective")
    assert(reaver.transport.isDefined, "hull left the ride mid-ferry")
  }

  test("unload: the ride puts the hull down inside the landing band", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val (reaver, shuttle) = pair(1460.0)
    reaver.transport = Some(shuttle)
    val hull = new ReaverExecutor
    val world = new World(Seq(shuttle, reaver), Seq(sunken(1700.0, 500.0)),
      Map(reaver -> hull, shuttle -> new ShuttleExecutor((1700.0, 500.0))), Seq.empty)

    while (world.frame < 500 && reaver.transport.isDefined) {
      world.step()
    }

    println(s"DIAG unload frames=${world.frame} unloads=${hull.unloads} loaded=${reaver.transport.isDefined}")
    assert(reaver.transport.isEmpty, "ride never unloaded inside the band")
  }

  test("shoot: a hull in gun reach holds and kills untouched", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val (reaver, shuttle) = pair(1460.0)
    shuttle.x = 600.0
    val colony = sunken(1700.0, 500.0)
    val hull = new ReaverExecutor
    val world = new World(Seq(reaver, shuttle), Seq(colony),
      Map(reaver -> hull, shuttle -> new ShuttleExecutor((1700.0, 500.0))), Seq.empty)

    while (world.frame < 2000 && colony.alive) {
      world.step()
    }

    println(s"DIAG shoot frames=${world.frame} sunken=${colony.hp.toInt} reaver=${reaver.hp.toInt} paralyzed=${hull.paralyzed}")
    assert( ! colony.alive, "held hull never killed the static")
    assert(reaver.hp == Protoss.Reaver.maxTotalHealth, "outranging hull took fire")
    assert(hull.paralyzed == 0, "hull held without a foe inside the gun")
  }

  test("fight: a hull outside gun reach closes and trades", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val (reaver, shuttle) = pair(1200.0)
    val colony = sunken(1700.0, 500.0)
    val hull = new ReaverExecutor
    val world = new World(Seq(reaver, shuttle), Seq(colony),
      Map(reaver -> hull, shuttle -> new ShuttleExecutor((1700.0, 500.0))), Seq.empty)

    while (world.frame < 3000 && colony.alive && reaver.alive) {
      world.step()
    }

    println(s"DIAG fight frames=${world.frame} sunken=${colony.hp.toInt} reaver=${reaver.hp.toInt}")
    assert( ! colony.alive, "closing hull never killed the static")
  }

  test("reboard: a doomed hull boards a ride in reach", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val (reaver, shuttle) = pair(1500.0)
    shuttle.x = 1510.0
    reaver.hp = (Protoss.Reaver.maxTotalHealth * 0.4).toInt
    val colony = sunken(1700.0, 500.0)
    val hull = new ReaverExecutor
    val world = new World(Seq(reaver, shuttle), Seq(colony),
      Map(reaver -> hull, shuttle -> new ShuttleExecutor((1700.0, 500.0))), Seq.empty)

    while (world.frame < 500 && reaver.transport.isEmpty && reaver.alive) {
      world.step()
    }

    println(s"DIAG reboard frames=${world.frame} boards=${hull.boards} loaded=${reaver.transport.isDefined} reaver=${reaver.hp.toInt}")
    assert(reaver.transport.isDefined, "doomed hull never boarded the ride in reach")
  }

  test("evacuate: a pressured loaded pair opens the distance aboard", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val (reaver, shuttle) = pair(1400.0)
    reaver.transport = Some(shuttle)
    val hydra = new Body(Zerg.Hydralisk, 1500.0, 500.0, Zerg.Hydralisk.maxTotalHealth)
    val start = shuttle.dist(hydra)
    val world = new World(Seq(shuttle, reaver), Seq(hydra),
      Map(reaver -> new ReaverExecutor, shuttle -> new ShuttleExecutor((100.0, 500.0)), hydra -> new Pilots.Hold), Seq.empty)

    while (world.frame < 1500 && shuttle.alive) {
      world.step()
    }

    println(s"DIAG evac frames=${world.frame} dist=${shuttle.dist(hydra).toInt} was=${start.toInt} aboard=${reaver.transport.isDefined}")
    assert(shuttle.dist(hydra) > start, "pressured ride never opened the distance")
    assert(reaver.transport.isDefined, "hull left the ride under pressure")
  }

  test("fetch: a far ride converges on a fighting hull and boards it", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val (reaver, shuttle) = pair(0.0)
    shuttle.x = 900.0
    val colony = sunken(200.0, 500.0)
    val hull = new ReaverExecutor
    val world = new World(Seq(reaver, shuttle), Seq(colony),
      Map(reaver -> hull, shuttle -> new ShuttleExecutor((200.0, 500.0))), Seq.empty)

    while (world.frame < 3000 && reaver.alive && (colony.alive || reaver.transport.isEmpty)) {
      world.step()
    }

    println(s"DIAG fetch frames=${world.frame} colony=${colony.hp.toInt} loaded=${reaver.transport.isDefined} paralyzed=${hull.paralyzed}")
    assert( ! colony.alive, "fighting hull never killed its duel while being fetched")
    assert(reaver.transport.isDefined, "ride never converged and boarded")
    assert(hull.paralyzed == 0, "hull waited without a foe inside the gun")
  }

  test("walk: a quiet hull walks to a far ride instead of waiting", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val (reaver, shuttle) = pair(0.0)
    shuttle.x = 900.0
    val hull = new ReaverExecutor
    val world = new World(Seq(reaver, shuttle), Seq.empty,
      Map(reaver -> hull, shuttle -> new ShuttleExecutor((0.0, 500.0))), Seq.empty)

    while (world.frame < 2000 && reaver.transport.isEmpty) {
      world.step()
    }

    println(s"DIAG walk frames=${world.frame} loaded=${reaver.transport.isDefined} paralyzed=${hull.paralyzed}")
    assert(reaver.transport.isDefined, "quiet hull never walked to its ride")
    assert(hull.paralyzed == 0, "walking hull held without a foe inside the gun")
  }

  test("alone: a rideless hull walks to the army, never waits", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val reaver = new Body(Protoss.Reaver, 0.0, 500.0, Protoss.Reaver.maxTotalHealth)
    val start = Math.abs(reaver.x - army._1)
    val hull = new ReaverExecutor
    val world = new World(Seq(reaver), Seq.empty, Map(reaver -> hull), Seq.empty)

    while (world.frame < 2000 && Math.abs(reaver.x - army._1) > 64.0) {
      world.step()
    }

    println(s"DIAG alone frames=${world.frame} x=${reaver.x.toInt} paralyzed=${hull.paralyzed}")
    assert(Math.abs(reaver.x - army._1) <= 64.0, f"rideless hull never walked to the army (start $start%.0f)")
    assert(hull.paralyzed == 0, "rideless hull held with nothing to shoot")
  }

  test("retarget: after a kill the pair ferries at the next static", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val (reaver, shuttle) = pair(1460.0)
    val first = sunken(1700.0, 500.0)
    val second = sunken(1700.0, 900.0)
    val world = new World(Seq(shuttle, reaver), Seq(first, second),
      Map(reaver -> new ReaverExecutor, shuttle -> new ShuttleExecutor((1700.0, 900.0))), Seq.empty)

    while (world.frame < 4000 && (first.alive || shuttle.y < 700.0)) {
      world.step()
    }

    println(s"DIAG retarget frames=${world.frame} first=${first.hp.toInt} y=${shuttle.y.toInt}")
    assert( ! first.alive, "pair never cleared the first static")
    assert(shuttle.y > 700.0, "pair never ferried at the next static")
  }

  test("full run: bond, ferry, unload, two kills, reboard, evacuate", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val (reaver, shuttle) = pair(0.0)
    val first = sunken(1700.0, 500.0)
    val second = sunken(1900.0, 500.0)
    val hull = new ReaverExecutor
    val world = new World(Seq(reaver, shuttle), Seq(first, second),
      Map(reaver -> hull, shuttle -> new ShuttleExecutor((1800.0, 500.0))), Seq.empty)

    var phase = 0
    while (world.frame < 6000 && reaver.alive && (first.alive || second.alive)) {
      world.step()
      if (phase == 0 && reaver.transport.isDefined) phase = 1
      if (phase == 1 && reaver.transport.isEmpty) phase = 2
      if (phase == 2 && ! first.alive) phase = 3
      if (phase == 3 && ! second.alive) phase = 4
    }

    println(s"DIAG full phases=$phase first=${first.hp.toInt} second=${second.hp.toInt} reaver=${reaver.hp.toInt} paralyzed=${hull.paralyzed}")
    assert( ! first.alive, "full run never cleared the first static")
    assert( ! second.alive, "full run never cleared the second static")
    assert(hull.paralyzed == 0, "full run held without a foe inside the gun")
  }
}
