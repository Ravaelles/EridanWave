package starengine

import Micro.Targeting.FiltersRequired.TargetFilterFocus
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Suicide base-razing scenario, real numbers: two Dragoons walk into a
 * base with two fleeing SCVs screening a Pylon. The targeting veto is the
 * shipped Focus vote (unassigned non-static buildings get no vote unless
 * the attacker is on a suicide mission) - the pilot calls the real kernel,
 * so a green run proves the shipped rule, not a copy. Workers die first
 * (nearest), then the base falls; a vetoed Pylon standing under Dragoons
 * is the reproduced miss.
 */
class SuicideRazeSuite extends AnyFunSuite {

  private def vetoed(f: Body): Boolean =
    f.kind.isBuilding && ! TargetFilterFocus.allowsBuilding(
      isBuilding = f.kind.isBuilding,
      isStaticDefense = f.kind.isStaticDefense,
      isSuicideMission = true)

  test("a suicide squad razes the base after the workers") {
    val pylon = new Body(Protoss.Pylon, 0.0, 0.0, Protoss.Pylon.maxTotalHealth)
    val scv1 = new Body(Terran.SCV, 0.0, 480.0, Terran.SCV.maxTotalHealth)
    val scv2 = new Body(Terran.SCV, 60.0, 500.0, Terran.SCV.maxTotalHealth)
    val dragoon1 = new Body(Protoss.Dragoon, 0.0, 600.0, Protoss.Dragoon.maxTotalHealth)
    val dragoon2 = new Body(Protoss.Dragoon, 60.0, 600.0, Protoss.Dragoon.maxTotalHealth)

    val attackMove = (fallback: (Double, Double)) =>
      new Pilots.AttackMove(_.foes, (me, f) => ! vetoed(f), fallback)
    val pilots: Map[Body, Pilot] = Map(
      dragoon1 -> attackMove((0.0, 0.0)),
      dragoon2 -> attackMove((0.0, 0.0)),
      scv1 -> new Pilots.Flee(_.ours),
      scv2 -> new Pilots.Flee(_.ours),
      pylon -> new Pilots.Hold)
    val world = new World(Seq(dragoon1, dragoon2), Seq(scv1, scv2, pylon), pilots, Seq.empty)

    var workersDeadFrame: Option[Int] = None
    var pylonDeadFrame: Option[Int] = None
    var firstBlood: Option[String] = None
    // Dragoons barely outrun SCVs (5.0 vs 4.92 px/frame for real), so the
    // chase alone eats thousands of frames; the engine is instant, the
    // budget only bounds the loop, not the clock.
    while (world.frame < 6000 && (pylon.alive || scv1.alive || scv2.alive) && dragoon1.alive && dragoon2.alive) {
      world.step()
      if (firstBlood.isEmpty) {
        if ( ! scv1.alive) firstBlood = Some("scv1")
        else if ( ! scv2.alive) firstBlood = Some("scv2")
        else if ( ! pylon.alive) firstBlood = Some("pylon")
      }
      if (workersDeadFrame.isEmpty && ! scv1.alive && ! scv2.alive) workersDeadFrame = Some(world.frame)
      if (pylonDeadFrame.isEmpty && ! pylon.alive) pylonDeadFrame = Some(world.frame)
    }

    assert(workersDeadFrame.isDefined, "dragoons never cleared the fleeing workers")
    assert(pylonDeadFrame.isDefined, "dragoons never razed the pylon they stand on")
    assert(firstBlood.exists(_ != "pylon"), "dragoons shot the base before touching its workers")
    assert(dragoon1.alive && dragoon2.alive, "dragoons died to unarmed workers and a pylon")
  }
}
