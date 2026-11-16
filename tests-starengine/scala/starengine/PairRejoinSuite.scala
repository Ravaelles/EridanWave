package starengine

import Micro.Actions.Protoss.ReaverShuttle.{ReaverBoardGate, ReaverShuttleReboard}
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Pair rejoin, step by step: a Reaver fights on foot while its empty
 * ride sits elsewhere. The abandoning ride awaits forward while the
 * mate dies alone under the guns (the field report: three failed
 * boardings, then a lone stand until frame 443). The rejoining ride
 * fetches from far, hovers at board reach when close, and only awaits
 * with no mate on foot; the hull fights on, boards pressured, and
 * boards a nearby ride once nothing is left to hunt. Speeds, reaches
 * and damage are the real UnitType numbers; the board decision is the
 * shipped reboard gate.
 */
class PairRejoinSuite extends AnyFunSuite with StarEngineSuite {

  private val home = (0.0, 500.0)
  private val awaitPoint = (500.0, 500.0)

  /** Abandoning ride, production order pre-fix: await forward alone. */
  private class AwaitPilot extends Pilot {

    override def direct(me: Body, world: World): Directive = MoveTo(awaitPoint._1, awaitPoint._2)
  }

  /**
   * Rejoining ride, production order: flee only when hot, otherwise
   * close with the mate and hover at board reach; home with nobody on
   * foot. A far hover strands the pickup, so the ride always closes.
   */
  private class RejoinPilot extends Pilot {

    override def direct(me: Body, world: World): Directive = {
      val mate = world.ours.find(o => o.alive && o.kind == Protoss.Reaver && o.transport.isEmpty)
      mate match {
        case Some(m) =>
          val threat = world.foes.filter(f => f.alive && f.canHit(me)).sortBy(me.dist).headOption
          threat match {
            case Some(t) if me.dist(t) <= t.rangeAgainst(me) + 32.0 =>
              val dx = me.x - t.x
              val dy = me.y - t.y
              val dist = Math.sqrt(dx * dx + dy * dy)
              if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)
              else Hold
            case _ if me.dist(m) > 64.0 => MoveTo(m.x, m.y)
            case _ => Hold
          }
        case None => MoveTo(home._1, home._2)
      }
    }
  }

  /** On-foot hull: wait for an incoming ride, fight the nearest foe,
    * board pressured (or a nearby ride with nothing to hunt) under the
    * shipped gate. Mirrors the production claims including the temporary
    * board lock: a quiet crowded hull does not embark. */
  private class FootPilot(waitForRide: Boolean) extends Pilot {
    var boards = 0
    var lastAttackFrame = -100000

    override def direct(me: Body, world: World): Directive = {
      val ride = world.ours.find(o => o.alive && o.kind == Protoss.Shuttle)
      if (me.loaded) {
        Hold
      } else {
        val foes = world.foes.filter(_.alive)
        val nearestTiles = foes.sortBy(me.dist).headOption.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity)
        val locked = ReaverBoardGate.locked(world.frame - lastAttackFrame, nearestTiles)
        val threat = foes.filter(f => f.canHit(me)).sortBy(me.dist).headOption
        val band = threat.exists(t =>
          ReaverShuttleReboard.mayReboard(me.dist(t) / 32.0, me.lossFraction))
        val seat = ride.filter(r => me.dist(r) <= 5.0 * 32.0 && ! locked && (band || foes.forall(f => me.dist(f) > me.rangeAgainst(f) + 64.0)))
        seat match {
          case Some(r) =>
            boards += 1

            Board(r)
          case None if waitForRide && ride.exists(r => me.dist(r) > 64.0) && foes.forall(f => me.dist(f) > me.rangeAgainst(f) + 64.0) =>
            Hold
          case None =>
            foes.sortBy(me.dist).headOption match {
              case Some(f) =>
                if (me.dist(f) <= me.rangeAgainst(f) + me.speed) lastAttackFrame = world.frame

                Chase(f)
              case None => Hold
            }
        }
      }
    }
  }

  private def runPair(ridePilot: Pilot, waitForRide: Boolean, shuttleStart: (Double, Double)): (Body, Body, FootPilot, Int) = {
    val shuttle = new Body(Protoss.Shuttle, shuttleStart._1, shuttleStart._2, Protoss.Shuttle.maxTotalHealth)
    val reaver = new Body(Protoss.Reaver, 800.0, 500.0, Protoss.Reaver.maxTotalHealth)
    val sunken = new Body(Zerg.SunkenColony, 1400.0, 500.0, Zerg.SunkenColony.maxTotalHealth)
    val hydra1 = new Body(Zerg.Hydralisk, 2000.0, 500.0, Zerg.Hydralisk.maxTotalHealth)
    val hydra2 = new Body(Zerg.Hydralisk, 2060.0, 540.0, Zerg.Hydralisk.maxTotalHealth)
    val hydra3 = new Body(Zerg.Hydralisk, 2000.0, 560.0, Zerg.Hydralisk.maxTotalHealth)
    val hydra4 = new Body(Zerg.Hydralisk, 2060.0, 580.0, Zerg.Hydralisk.maxTotalHealth)
    val foot = new FootPilot(waitForRide)
    val pilots: Map[Body, Pilot] = Map(
      shuttle -> ridePilot,
      reaver -> foot,
      sunken -> new Pilots.Hold,
      hydra1 -> new Pilots.ZombieAttack(_.ours, (1400.0, 500.0)),
      hydra2 -> new Pilots.ZombieAttack(_.ours, (1400.0, 500.0)),
      hydra3 -> new Pilots.ZombieAttack(_.ours, (1400.0, 500.0)),
      hydra4 -> new Pilots.ZombieAttack(_.ours, (1400.0, 500.0)))
    val world = new World(Seq(shuttle, reaver), Seq(sunken, hydra1, hydra2, hydra3, hydra4), pilots, Seq.empty)

    while (world.frame < 6000 && reaver.alive && shuttle.alive && reaver.transport.isEmpty) {
      world.step()
    }

    (reaver, shuttle, foot, world.frame)
  }

  test("an awaiting ride watches its mate die alone", StarEngineTags.Reaver, StarEngineTags.Shuttle, StarEngineTags.Board) {
    val (reaver, shuttle, foot, frames) = runPair(new AwaitPilot, waitForRide = false, home)
    println(s"DIAG abandon frames=$frames boards=${foot.boards} reaver=${reaver.hp.toInt} loaded=${reaver.transport.isDefined}")
    assert( ! reaver.alive, "abandoned hull survived - no problem to fix")
    assert(foot.boards == 0, "abandoned hull embarked with no ride around")
  }

  test("a rejoining ride is there when the hull boards pressured", StarEngineTags.Reaver, StarEngineTags.Shuttle, StarEngineTags.Board) {
    val (reaver, shuttle, foot, frames) = runPair(new RejoinPilot, waitForRide = true, (-800.0, 500.0))
    println(s"DIAG rejoin frames=$frames boards=${foot.boards} reaver=${reaver.hp.toInt} loaded=${reaver.transport.isDefined} shuttle=${shuttle.hp.toInt}")
    assert(foot.boards > 0, "rejoined hull never boarded under pressure")
    assert(reaver.alive, "rejoined hull died with the ride around")
    assert(shuttle.alive, "ride died hovering the mate")
  }
}
