package starengine

import Micro.Actions.Combat.Decisioning.CombatSituation
import Micro.Targeting.WorkerSpread
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Worker spread, step by step: two Dragoons meet three mining SCVs. The
 * pilots call the shipped WorkerSpread deal on the same facts production
 * reads (id-sorted hittable workers, attacker id) and each takes its own
 * victim instead of queuing the whole line on the weakest one - the deal
 * is stable across frames, so nobody re-picks mid-duel. Speeds, reaches
 * and damage are the real UnitType numbers.
 */
class WorkerSpreadScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Targeting.name) }

  /** Pilot, production order: the shipped kernel deals, legs execute. */
  private class SpreadPilot(seed: Int) extends Pilot {
    var picks = Set.empty[Int]

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      if (foes.isEmpty) return Hold

      val hittable = foes.filter(f => f.kind.isWorker && me.dist(f) <= me.rangeAgainst(f)).sortBy(System.identityHashCode(_)).toVector
      val pick =
        if (hittable.size >= 2) hittable(WorkerSpread.pickSlot(seed, hittable.size))
        else foes.minBy(me.dist)
      picks += System.identityHashCode(pick)

      Chase(pick)
    }
  }

  test("two shooters deal the line instead of queuing one victim") {
    val dragoonA = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val dragoonB = new Body(Protoss.Dragoon, 60.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val scvs = (0 until 3).map(i =>
      new Body(Terran.SCV, 300.0 + i * 30.0, 500.0, Terran.SCV.maxTotalHealth)).toSeq
    val pilotA = new SpreadPilot(10)
    val pilotB = new SpreadPilot(11)
    val world = new World(Seq(dragoonA, dragoonB), scvs,
      Map(dragoonA -> pilotA, dragoonB -> pilotB) ++ scvs.map(_ -> new Pilots.Hold).toMap, Seq.empty)

    var f = 0
    while (f < 500 && dragoonA.alive && scvs.exists(_.alive)) {
      world.step()
      f = world.frame
    }
    println(s"DIAG spread A=${pilotA.picks} B=${pilotB.picks} alive=${scvs.count(_.alive)}/3")
    assert(pilotA.picks.nonEmpty && pilotB.picks.nonEmpty, "a shooter never picked")
    assert(pilotA.picks != pilotB.picks || scvs.count(_.alive) <= 1, "both shooters queued the same victim")
  }
}
