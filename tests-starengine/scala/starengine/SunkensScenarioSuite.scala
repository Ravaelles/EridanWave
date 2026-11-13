package starengine

import Micro.Targeting.FiltersRequired.TargetFilterFocus
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Sunkens escort-first scenario, real numbers: a Reaver plus its Shuttle
 * walk into two Zerglings screening a Sunken Colony. Speeds, reaches,
 * damage and cooldowns come from the unit classes (ling 5.49 px/frame
 * outruns the Reaver's 1.78 - standing ground and shooting nearest is
 * the only winning shape). The ling leash is the UMS map rule: no ling
 * may stray past 15 tiles from the Sunken, crossed the line it teleports
 * back near it. Workers (lings) die first, then the static, the ride
 * never takes a hit.
 */
class SunkensScenarioSuite extends AnyFunSuite {

  private def vetoed(f: Body): Boolean =
    f.kind.isBuilding && ! TargetFilterFocus.allowsBuilding(
      isBuilding = f.kind.isBuilding,
      isStaticDefense = f.kind.isStaticDefense,
      isSuicideMission = false)

  test("a reaver clears its ling escort before the sunken") {
    val sunken = new Body(Zerg.SunkenColony, 0.0, 0.0, Zerg.SunkenColony.maxTotalHealth)
    val ling1 = new Body(Zerg.Zergling, 0.0, 480.0, Zerg.Zergling.maxTotalHealth)
    val ling2 = new Body(Zerg.Zergling, 96.0, 576.0, Zerg.Zergling.maxTotalHealth)
    val reaver = new Body(Protoss.Reaver, 0.0, 1024.0, Protoss.Reaver.maxTotalHealth)
    val shuttle = new Body(Protoss.Shuttle, 0.0, 1024.0, Protoss.Shuttle.maxTotalHealth)

    val attackMove = (fallback: (Double, Double)) =>
      new Pilots.AttackMove(_.foes, (me, f) => ! vetoed(f), fallback)
    val pilots: Map[Body, Pilot] = Map(
      reaver -> attackMove((0.0, 0.0)),
      shuttle -> new Pilots.Hold,
      ling1 -> new Pilots.AttackMove(_.ours, (_, _) => true, (0.0, 0.0)),
      ling2 -> new Pilots.AttackMove(_.ours, (_, _) => true, (0.0, 0.0)),
      sunken -> new Pilots.Hold)
    val leash = new Constraints.StaticLeash(
      maxTiles = 15.0,
      nearTiles = 2.0,
      isBound = _.kind == Zerg.Zergling,
      isAnchor = _.kind == Zerg.SunkenColony)
    val world = new World(Seq(reaver, shuttle), Seq(ling1, ling2, sunken), pilots, Seq(leash))

    var lingsDeadFrame: Option[Int] = None
    var sunkenDeadFrame: Option[Int] = None
    while (world.frame < 8000 && (sunken.alive || ling1.alive || ling2.alive) && reaver.alive) {
      world.step()
      if (lingsDeadFrame.isEmpty && ! ling1.alive && ! ling2.alive) lingsDeadFrame = Some(world.frame)
      if (sunkenDeadFrame.isEmpty && ! sunken.alive) sunkenDeadFrame = Some(world.frame)
    }

    assert(lingsDeadFrame.isDefined, "reaver never cleared the ling escort")
    assert(sunkenDeadFrame.isDefined, "reaver never killed the sunken")
    assert(lingsDeadFrame.get < sunkenDeadFrame.get, "reaver shot the static before clearing its escort")
    assert(reaver.alive, "reaver died clearing two lings and a sunken")
    assert(shuttle.hp == shuttle.maxHp, "shuttle took hits holding off the statics")
    assert(reaver.lossFraction < 0.15, f"reaver lost ${reaver.lossFraction * 100}%.1f%% - escort-first broke down")
  }
}
