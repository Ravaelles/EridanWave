package starengine

import Micro.Actions.Protoss.ReaverShuttle.PairArbiter
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

  /** Shuttle executor: the arbiter's order, nothing else. */
  private class ShuttlePilot extends Pilot {
    var fetches = 0
    var flees = 0
    var idles = 0

    override def direct(me: Body, world: World): Directive = {
      val mate = world.ours.find(o => o.alive && o.kind == Protoss.Reaver && o.transport.isEmpty)
      val foes = world.foes.filter(_.alive)
      val pressured = foes.exists(f =>
        f.canHit(me) && me.dist(f) <= f.rangeAgainst(me) + 5.0 * 32.0)
      val out = PairArbiter.decide(
        loaded           = world.ours.exists(o => o.alive && o.kind == Protoss.Reaver && o.loaded),
        mateOnFoot       = mate.exists(m => ! m.loaded),
        mateDistTiles    = mate.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity),
        reaverFoeTiles   = Double.PositiveInfinity,
        reaverGunTiles   = Double.PositiveInfinity,
        reaverShotReady  = false,
        shuttlePressured = pressured,
        reaverDoomed     = false,
        raid             = false)
      out.shuttle match {
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
      val ride = world.ours.find(o => o.alive && o.kind == Protoss.Shuttle)
      val foes = world.foes.filter(_.alive)
      val nearest = foes.sortBy(me.dist).headOption
      val foeTiles = nearest.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity)
      val gunTiles = nearest.map(me.rangeAgainst(_) / 32.0).getOrElse(Double.PositiveInfinity)
      val out = PairArbiter.decide(
        loaded           = me.loaded,
        mateOnFoot       = ! me.loaded,
        mateDistTiles    = ride.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity),
        reaverFoeTiles   = foeTiles,
        reaverGunTiles   = gunTiles,
        reaverShotReady  = me.cooldownLeft <= 0,
        shuttlePressured = false,
        reaverDoomed     = me.lossFraction > 0.75,
        raid             = false)
      out.reaver match {
        case PairArbiter.BoardRide =>
          ride match {
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
          ride.map(r => MoveTo(r.x, r.y)).getOrElse(Hold)
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
