package starengine

import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Prey drive, step by step: a Dragoon finds two mining SCVs and nothing
 * else. The timid pilot mirrors the old rule - no consensus against
 * workers, so it holds its slot and mills while they mine. The prey
 * pilot calls the shipped PreyDrive on the same facts production reads
 * (workers-only contacts) and hunts instead: every frame closes or
 * shoots, none walks home. Speeds, reaches and damage are the real
 * UnitType numbers; the hunt decision is the shipped kernel.
 */
class WorkerHuntSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Hunt.name + StarEngineTags.Dragoon.name) }

  /** Timid pilot, old rule: workers are not a fight, hold the slot. */
  private class TimidPilot extends Pilot {
    var holds = 0

    override def direct(me: Body, world: World): Directive = {
      holds += 1

      Hold
    }
  }

  /**
   * Prey pilot, production order: workers-only contacts hunt, so chase
   * the nearest foe; the shipped kernel owns the decision, the pilot
   * only executes it.
   */
  private class PreyPilot extends Pilot {
    var retreats = 0

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      val workersOnly = foes.nonEmpty && foes.forall(_.kind.isWorker)
      val hunt = Micro.Actions.Combat.Decisioning.PreyDrive.shouldHunt(workersOnly, 0.0, 0.0)
      if ( ! hunt) {
        retreats += 1

        return Hold
      }
      foes.sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)
    }
  }

  private def runDuel(pilot: Pilot): (Body, Seq[Body], Int) = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val scvs = Seq(
      new Body(Terran.SCV, 300.0, 500.0, Terran.SCV.maxTotalHealth),
      new Body(Terran.SCV, 360.0, 510.0, Terran.SCV.maxTotalHealth))
    val world = new World(Seq(dragoon), scvs,
      Map(dragoon -> pilot) ++ scvs.map(_ -> new Pilots.Hold).toMap, Seq.empty)

    while (world.frame < 3000 && dragoon.alive && scvs.exists(_.alive)) {
      world.step()
    }

    (dragoon, scvs, world.frame)
  }

  test("a timid hull holds its slot while workers mine") {
    val pilot = new TimidPilot
    val (dragoon, scvs, frames) = runDuel(pilot)
    println(s"DIAG timid frames=$frames holds=${pilot.holds} dragoon=${dragoon.hp.toInt} scvs=${scvs.count(_.alive)}/2")
    assert(scvs.forall(_.alive), "timid hull killed a worker - no milling to fix")
  }

  test("a prey-driven hull hunts workers instead of posing") {
    val pilot = new PreyPilot
    val (dragoon, scvs, frames) = runDuel(pilot)
    println(s"DIAG hunt frames=$frames retreats=${pilot.retreats} dragoon=${dragoon.hp.toInt} scvs=${scvs.count(_.alive)}/2")
    assert(pilot.retreats == 0, "hunter walked home instead of hunting")
    assert(scvs.forall(! _.alive), "hunter never cleared the workers")
    assert(dragoon.alive, "hunter died killing two workers")
  }
}
