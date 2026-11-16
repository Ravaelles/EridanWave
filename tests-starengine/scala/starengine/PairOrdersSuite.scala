package starengine

import Micro.Actions.Protoss.ReaverShuttle.{PairArbiter, ReaverBoardGate, ReaverShuttleFerry}
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Pair orders, step by step: a Reaver fights on foot while its Shuttle
 * crosses the map, and a loaded pair meets air pressure. Both pilots
 * call the shipped PairArbiter every frame - one snapshot, one order
 * per hull, no negotiation between the brains. The fighting mate is
 * fetched (never left to wait), the hull boards inside rejoin reach,
 * and the pressured loaded pair flees with the hull aboard. Speeds,
 * reaches and damage are the real UnitType numbers; the orders are
 * the shipped kernel, the moves and embarks are the engine's.
 */
class PairOrdersSuite extends AnyFunSuite with StarEngineSuite {

  private val army = (100.0, 500.0)

  private def tilePos(b: Body): (Double, Double) = (b.x / 32.0, b.y / 32.0)

  private def foeViews(me: Body, foes: Seq[Body]): Seq[PairArbiter.FoeView] =
    foes.filter(_.alive).map(f => PairArbiter.FoeView(
      me.dist(f) / 32.0,
      f.rangeAgainst(me) / 32.0,
      me.rangeAgainst(f) / 32.0))

  private def staticTiles(me: Body, foes: Seq[Body]): Option[Double] =
    foes.filter(f => f.alive && f.kind.isBuilding).sortBy(me.dist).headOption
      .map(me.dist(_) / 32.0)

  /**
   * Inferred shot clock, production order: the engine never tells the
   * pilot it fired, so a foe inside the gun means the clock resets.
   */
  private val lastShot = new scala.collection.mutable.HashMap[Body, Int]().withDefaultValue(-100000)

  private def boardLocked(me: Body, foes: Seq[Body], frame: Int): Boolean = {
    val live = foes.filter(_.alive)
    if (live.exists(f => me.dist(f) <= me.rangeAgainst(f))) lastShot(me) = frame
    val crowd = live.filter(_.canHit(me)).sortBy(me.dist).headOption
      .map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity)

    ReaverBoardGate.locked(frame - lastShot(me), crowd)
  }

  /**
   * Shuttle executor, production path: bodies become the same plain
   * views the game seam builds, then the shipped resolve runs. Any
   * drift between the test and the game lives in the view mapping,
   * never in the decision.
   */
  private class ShuttlePilot extends Pilot {
    var fetches = 0
    var flees = 0
    var idles = 0

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      val mates = world.ours.view.filter(o => o.alive && o.kind == Protoss.Reaver).map(r =>
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
          staticTiles(r, foes),
          r.lossFraction > 0,
          boardLocked(r, foes, world.frame),
          Double.PositiveInfinity,
          false,
          foeViews(r, foes))).toSeq
      val out = PairArbiter.resolveShuttle(
        PairArbiter.ShuttleView(
          tilePos(me),
          Some(1),
          mates.exists(_.loaded),
          foes.filter(f => f.canHit(me)).map(f =>
            PairArbiter.ThreatView(me.dist(f) / 32.0, f.rangeAgainst(me) / 32.0))),
        mates,
        raid = false,
        baitRide = false).shuttle
      val mate = world.ours.find(o => o.alive && o.kind == Protoss.Reaver && o.transport.isEmpty)
      out match {
        case PairArbiter.FetchMate =>
          fetches += 1
          mate.map(m => MoveTo(m.x, m.y)).getOrElse(Hold)
        case PairArbiter.GuardMate =>
          mate.map(m => MoveTo(m.x, m.y)).getOrElse(Hold)
        case PairArbiter.Flee =>
          flees += 1
          foes.sortBy(me.dist).headOption match {
            case Some(t) =>
              val dx = me.x - t.x
              val dy = me.y - t.y
              val dist = Math.sqrt(dx * dx + dy * dy)
              if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)
              else Hold
            case None => Hold
          }
        case _ =>
          idles += 1

          Hold
      }
    }
  }

  /**
   * Reaver executor, production order: the arbiter's order, nothing
   * else. Counts paralyzed frames - a Hold with no foe inside the gun
   * is the wait this arbiter was built to ban.
   */
  private class ReaverPilot extends Pilot {
    var boards = 0
    var paralyzed = 0

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      val nearest = foes.sortBy(me.dist).headOption
      val foeTiles = nearest.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity)
      val gunTiles = nearest.map(me.rangeAgainst(_) / 32.0).getOrElse(Double.PositiveInfinity)
      val rides = world.ours.view.filter(o => o.alive && o.kind == Protoss.Shuttle).map(s =>
        PairArbiter.ShuttleView(
          tilePos(s),
          Some(1),
          world.ours.exists(o => o.alive && o.kind == Protoss.Reaver && o.loaded && o.transport.contains(s)),
          world.foes.view.filter(f => f.alive && f.canHit(s)).map(f =>
            PairArbiter.ThreatView(s.dist(f) / 32.0, f.rangeAgainst(s) / 32.0)).toSeq)).toSeq
      val rideBody = world.ours.find(o => o.alive && o.kind == Protoss.Shuttle)
      val out = PairArbiter.resolveReaver(
        PairArbiter.ReaverView(
          tilePos(me),
          Some(1),
          me.loaded,
          rideBody.map(r => PairArbiter.ShuttleView(
            tilePos(r),
            Some(1),
            me.loaded,
            world.foes.view.filter(f => f.alive && f.canHit(r)).map(f =>
              PairArbiter.ThreatView(r.dist(f) / 32.0, f.rangeAgainst(r) / 32.0)).toSeq)),
          1,
          activeScarab = false,
          me.cooldownLeft,
          me.lossFraction,
          unloadOrdered = false,
          staticTiles(me, foes),
          me.lossFraction > 0,
          boardLocked(me, foes, world.frame),
          foes.filter(_.canHit(me)).sortBy(me.dist).headOption.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity),
          ReaverShuttleFerry.needsRide(Math.hypot(me.x - army._1, me.y - army._2) / 32.0),
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

  test("a fighting mate is fetched, fights, and boards inside reach", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val reaver = new Body(Protoss.Reaver, 0.0, 500.0, Protoss.Reaver.maxTotalHealth)
    val marine = new Body(Terran.Marine, 200.0, 500.0, Terran.Marine.maxTotalHealth)
    val shuttle = new Body(Protoss.Shuttle, 900.0, 500.0, Protoss.Shuttle.maxTotalHealth)
    val shuttlePilot = new ShuttlePilot
    val reaverPilot = new ReaverPilot
    val world = new World(
      Seq(reaver, shuttle),
      Seq(marine),
      Map(reaver -> reaverPilot, shuttle -> shuttlePilot, marine -> new Pilots.Hold),
      Seq.empty)

    while (world.frame < 3000 && reaver.alive && (marine.alive || reaver.transport.isEmpty)) {
      world.step()
    }

    println(s"DIAG fetch frames=${world.frame} fetches=${shuttlePilot.fetches} boards=${reaverPilot.boards} paralyzed=${reaverPilot.paralyzed} marine=${marine.hp.toInt} loaded=${reaver.transport.isDefined}")
    assert( ! marine.alive, "fighting mate never killed its duel while being fetched")
    assert(reaver.transport.isDefined, "mate never boarded the fetched ride")
    assert(reaverPilot.paralyzed == 0, "hull held without a foe inside the gun")
  }

  test("a pressured loaded pair flees with the hull aboard", StarEngineTags.Reaver, StarEngineTags.Shuttle) {
    val shuttle = new Body(Protoss.Shuttle, 500.0, 500.0, Protoss.Shuttle.maxTotalHealth)
    val reaver = new Body(Protoss.Reaver, 500.0, 500.0, Protoss.Reaver.maxTotalHealth)
    val wraith = new Body(Terran.Wraith, 700.0, 500.0, Terran.Wraith.maxTotalHealth)
    val shuttlePilot = new ShuttlePilot
    val world = new World(
      Seq(shuttle, reaver),
      Seq(wraith),
      Map(shuttle -> shuttlePilot, reaver -> new Pilots.Hold, wraith -> new Pilots.Hold),
      Seq.empty)
    world.step()
    // Embark first: the pair starts bonded.
    reaver.transport = Some(shuttle)

    val startDist = shuttle.dist(wraith)
    while (world.frame < 1500 && shuttle.alive) {
      world.step()
    }

    println(s"DIAG flee frames=${world.frame} flees=${shuttlePilot.flees} dist=${shuttle.dist(wraith).toInt} was=${startDist.toInt} aboard=${reaver.transport.isDefined}")
    assert(shuttlePilot.flees > 0, "pressured ride never took the flee order")
    assert(shuttle.dist(wraith) > startDist, "fleeing ride never opened the distance")
    assert(reaver.transport.isDefined, "hull left the ride under pressure")
    assert(shuttle.alive, "ride died fleeing a slower threat")
  }
}
