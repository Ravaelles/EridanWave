package starengine

import Micro.Actions.Protoss.ReaverShuttle.{ReaverBoardGate, ReaverShuttleReboard}
import Micro.Actions.Transportation.DropSafety
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Board/drop matrix: the allow and deny legs side by side. A pressured
 * hull does not chase a far ride (it fights where it stands), nothing
 * embarks onto a dead hull, and a ride does not unload into static
 * cover (the shipped DropSafety veto). Pilots mirror the production
 * order: the shipped board gate, the shipped rejoin reach and the
 * shipped landing veto. Speeds, reaches and damage are the real
 * UnitType numbers.
 */
class BoardDropMatrixSuite extends AnyFunSuite with StarEngineSuite {

  /** On-foot hull with the shipped gate and reach, no waiting. */
  private class MatrixFootPilot extends Pilot {
    var boards = 0
    var bandSeen = false
    var lastAttackFrame: Int = -100000

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
        if (band) bandSeen = true
        val seat = ride.filter(r =>
          ReaverShuttleReboard.rideInReach(me.dist(r) / 32.0) && ! locked && band)
        seat match {
          case Some(r) =>
            boards += 1

            Board(r)
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

  test("a pressured hull fights in place instead of chasing a far ride",
    StarEngineTags.Reaver, StarEngineTags.Shuttle, StarEngineTags.Board) {
    val shuttle = new Body(Protoss.Shuttle, 0.0, 500.0, Protoss.Shuttle.maxTotalHealth)
    val reaver = new Body(Protoss.Reaver, 800.0, 500.0, Protoss.Reaver.maxTotalHealth / 2)
    val hydra = new Body(Zerg.Hydralisk, 850.0, 500.0, Zerg.Hydralisk.maxTotalHealth)
    val hydra2 = new Body(Zerg.Hydralisk, 1100.0, 540.0, Zerg.Hydralisk.maxTotalHealth)
    val foot = new MatrixFootPilot
    val pilots: Map[Body, Pilot] = Map(
      shuttle -> new Pilots.Hold,
      reaver -> foot,
      hydra -> new Pilots.ZombieAttack(_.ours, (800.0, 500.0)),
      hydra2 -> new Pilots.ZombieAttack(_.ours, (800.0, 500.0)))
    val world = new World(Seq(shuttle, reaver), Seq(hydra, hydra2), pilots, Seq.empty)

    while (world.frame < 3000 && reaver.alive && (hydra.alive || hydra2.alive)) {
      world.step()
    }

    println(s"DIAG farride frames=${world.frame} boards=${foot.boards} band=${foot.bandSeen} reaver=${reaver.hp.toInt} hydras=${hydra.hp.toInt},${hydra2.hp.toInt}")
    assert(foot.bandSeen, "band never fired - the distance gate went untested")
    assert(foot.boards == 0, "hull chased a ride 800px away instead of fighting")
    assert( ! hydra.alive && ! hydra2.alive, "hull never fought the pressers")
    assert(reaver.alive, "hull died fighting alone")
  }

  test("nothing embarks onto a dead ride",
    StarEngineTags.Reaver, StarEngineTags.Shuttle, StarEngineTags.Board) {
    val wreck = new Body(Protoss.Shuttle, 32.0, 500.0, 0.0)
    val reaver = new Body(Protoss.Reaver, 0.0, 500.0, Protoss.Reaver.maxTotalHealth / 2)
    val hydra = new Body(Zerg.Hydralisk, 50.0, 500.0, Zerg.Hydralisk.maxTotalHealth)
    val hydra2 = new Body(Zerg.Hydralisk, 300.0, 540.0, Zerg.Hydralisk.maxTotalHealth)
    val foot = new MatrixFootPilot
    val pilots: Map[Body, Pilot] = Map(
      wreck -> new Pilots.Hold,
      reaver -> foot,
      hydra -> new Pilots.ZombieAttack(_.ours, (0.0, 500.0)),
      hydra2 -> new Pilots.ZombieAttack(_.ours, (0.0, 500.0)))
    val world = new World(Seq(wreck, reaver), Seq(hydra, hydra2), pilots, Seq.empty)

    while (world.frame < 3000 && reaver.alive && (hydra.alive || hydra2.alive)) {
      world.step()
    }

    println(s"DIAG wreck frames=${world.frame} boards=${foot.boards} band=${foot.bandSeen} reaver=${reaver.hp.toInt} hydras=${hydra.hp.toInt},${hydra2.hp.toInt}")
    assert(foot.bandSeen, "band never fired - the wreck guard went untested")
    assert(foot.boards == 0, "hull boarded a wreck")
    assert( ! hydra.alive && ! hydra2.alive, "hull never fought the pressers")
  }

  test("a covered drop stays aboard",
    StarEngineTags.Reaver, StarEngineTags.Shuttle, StarEngineTags.Drop) {
    val shuttle = new Body(Protoss.Shuttle, 0.0, 500.0, Protoss.Shuttle.maxTotalHealth)
    val reaver = new Body(Protoss.Reaver, 0.0, 500.0, Protoss.Reaver.maxTotalHealth)
    reaver.transport = Some(shuttle)
    val turret = new Body(Terran.MissileTurret, 1700.0, 500.0, Terran.MissileTurret.maxTotalHealth)
    val goal = (1700.0, 500.0)
    var unloads = 0
    val ride = new Pilot {

      override def direct(me: Body, world: World): Directive = {
        val statics = world.foes.filter(f => f.alive && f.kind.isBuilding)
        val landingTiles = statics.sortBy(f => math.hypot(f.x - goal._1, f.y - goal._2)).headOption
          .map(f => math.hypot(f.x - goal._1, f.y - goal._2) / 32.0)
        if (DropSafety.vetoLanding(landingTiles)) Hold
        else if (math.hypot(me.x - goal._1, me.y - goal._2) > 64.0) MoveTo(goal._1, goal._2)
        else Hold
      }
    }
    val hull = new Pilot {

      override def direct(me: Body, world: World): Directive = {
        if (me.loaded) {
          val near = world.foes.filter(f => f.alive && f.kind.isBuilding)
            .sortBy(f => math.hypot(f.x - goal._1, f.y - goal._2)).headOption
            .map(f => math.hypot(f.x - goal._1, f.y - goal._2) / 32.0)
          if ( ! DropSafety.vetoLanding(near)) {
            unloads += 1

            Unload
          } else Hold
        } else Hold
      }
    }
    val pilots: Map[Body, Pilot] = Map(
      shuttle -> ride,
      reaver -> hull,
      turret -> new Pilots.Hold)
    val world = new World(Seq(shuttle, reaver), Seq(turret), pilots, Seq.empty)

    while (world.frame < 3000 && reaver.alive && shuttle.alive) {
      world.step()
    }

    println(s"DIAG covered frames=${world.frame} unloads=$unloads reaver=${reaver.hp.toInt} shuttle=${shuttle.hp.toInt} loaded=${reaver.transport.isDefined}")
    assert(unloads == 0, "ride unloaded into static cover")
    assert(reaver.transport.isDefined, "cargo left the hull over guns")
    assert(reaver.alive && shuttle.alive, "pair died holding off the guns")
  }
}
