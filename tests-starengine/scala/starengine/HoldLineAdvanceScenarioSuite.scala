package starengine

import Micro.Actions.Combat.Decisioning.HoldLineAdvance
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * HoldLine creep (the per-unit anchor): a dressed ranged line that is
 * holding walks its formation at the enemy in fixed micro-steps
 * instead of freezing until the timeout. The anchor persists across
 * frames and only advances - the hull never re-aims from wherever it
 * drifted and never walks back. The pilot below mirrors the production
 * behavior (shipped shouldAdvance gate and stepTiles, anchor advance and
 * release); the suite pins the contract temporally: steady advance at
 * the creep rate, no retreat, clean release when the target closes.
 */
class HoldLineAdvanceScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Formation.name) }

  private class CreepPilot extends Pilot {
    var anchor: Option[(Double, Double)] = None
    var minX: Double = Double.PositiveInfinity

    override def direct(me: Body, world: World): Directive = {
      val foe = world.foes.find(_.alive)
      val beyondTiles = foe.map(f => (me.dist(f) - me.rangeAgainst(f)) / 32.0).getOrElse(-1.0)
      val creeping = foe.nonEmpty && HoldLineAdvance.shouldAdvance(
        hasFormation   = true,
        melee          = false,
        weaponReady    = me.cooldownLeft <= 0,
        fractionInSlot = 1.0,
        beyondRangeTiles = beyondTiles)
      if ( ! creeping) {
        anchor = None

        return Hold
      }

      val a = anchor.getOrElse((me.x, me.y))
      val t = foe.get
      val dx = t.x - a._1
      val dy = t.y - a._2
      val dist = Math.hypot(dx, dy)
      val step = HoldLineAdvance.stepTiles(beyondTiles) * 32.0
      val next = if (dist <= 0) a else (a._1 + dx / dist * Math.min(dist, step), a._2 + dy / dist * Math.min(dist, step))
      anchor = Some(next)
      if (me.x < minX) minX = me.x
      MoveTo(next._1, next._2)
    }
  }

  test("a dressed line creeps forward without ever stepping back") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    dragoon.cooldownLeft = 0
    val foe = new Body(Terran.Marine, 320.0, 500.0, Terran.Marine.maxTotalHealth)
    foe.passive = true
    val pilot = new CreepPilot
    val world = new World(Seq(dragoon), Seq(foe),
      Map(dragoon -> pilot, foe -> new Pilots.Hold), Seq.empty)
    var f = 0
    while (f < 300 && dragoon.alive) { world.step(); f = world.frame }
    val advanced = dragoon.x - 0.0
    val finalDist = foe.x - dragoon.x
    println(s"DIAG advanced=${advanced.toInt} minX=${pilot.minX.toInt} finalDist=${finalDist.toInt} frames=$f")
    assert(advanced > 100, f"line froze instead of creeping: advanced $advanced in $f frames")
    assert(pilot.minX >= -1.0, f"line stepped back to ${pilot.minX}")
    assert(finalDist >= 64 && finalDist <= 200, f"line broke instead of holding: stopped $finalDist from the target")
  }

  test("the anchor releases when the target closes into range") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    dragoon.cooldownLeft = 0
    val foe = new Body(Terran.Marine, 100.0, 500.0, Terran.Marine.maxTotalHealth)
    foe.passive = true
    val pilot = new CreepPilot
    val world = new World(Seq(dragoon), Seq(foe),
      Map(dragoon -> pilot, foe -> new Pilots.Hold), Seq.empty)
    var f = 0
    while (f < 120 && dragoon.alive) { world.step(); f = world.frame }
    println(s"DIAG release anchor=${pilot.anchor} dragoonX=${dragoon.x.toInt}")
    assert(pilot.anchor.isEmpty, "anchor stuck after the target closed in range")
  }
}
