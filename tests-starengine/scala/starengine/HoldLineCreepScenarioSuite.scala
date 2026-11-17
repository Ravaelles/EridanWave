package starengine

import Micro.Actions.Combat.Decisioning.HoldLineCreep
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * HoldLine creep: a dressed ranged line that is holding walks its
 * formation at the enemy in fixed micro-steps instead of freezing until
 * the timeout or breaking into a charge. The pilot calls the shipped
 * gate and steps exactly the kernel's stride; the foe is a passive hull
 * that never shoots back, mirroring the UMS duels that stood still. The
 * line must close to its guns without a single charge stride, then open
 * fire on arrival - advance without appetite.
 */
class HoldLineCreepScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Formation.name) }

  private class CreepPilot(foe: Body) extends Pilot {
    var maxStep: Double = 0.0
    private var lastX: Double = Double.NaN
    private var lastY: Double = Double.NaN

    override def direct(me: Body, world: World): Directive = {
      if ( ! lastX.isNaN) maxStep = Math.max(maxStep, Math.hypot(me.x - lastX, me.y - lastY))
      lastX = me.x
      lastY = me.y
      if ( ! foe.alive) return Hold
      val beyondTiles = (me.dist(foe) - me.rangeAgainst(foe)) / 32.0
      if (HoldLineCreep.shouldCreep(
        hasFormation     = true,
        melee            = me.kind.melee,
        weaponReady      = me.cooldownLeft <= 0,
        fractionInSlot   = 1.0,
        beyondRangeTiles = beyondTiles)) {
        val step = HoldLineCreep.stepTiles(beyondTiles) * 32.0
        val dist = me.dist(foe)
        if (dist > 0) MoveTo(me.x + (foe.x - me.x) / dist * step, me.y + (foe.y - me.y) / dist * step)
        else Hold
      } else Hold
    }
  }

  test("a dressed line creeps to its guns without charging") {
    val line = new Body(Protoss.Dragoon, 0.0, -300.0, Protoss.Dragoon.maxTotalHealth)
    val hull = new Body(Protoss.Zealot, 0.0, 100.0, Protoss.Zealot.maxTotalHealth)
    hull.passive = true
    val pilot = new CreepPilot(hull)
    val world = new World(Seq(line), Seq(hull), Map(line -> pilot, hull -> new Pilots.Hold), Seq.empty)
    var f = 0
    while (f < 400 && hull.alive) { world.step(); f += 1 }
    val closed = 400.0 - line.dist(hull)
    println(s"DIAG closed=${closed.toInt}px maxStep=${pilot.maxStep.toString.take(4)}px dist=${line.dist(hull).toInt}px hullHp=${hull.hp.toInt}")
    assert(closed > 200.0, "the line never walked at the enemy")
    assert(pilot.maxStep <= 4.0, "a stride bigger than the creep step - the line charged")
    assert(line.dist(hull) <= line.rangeAgainst(hull) + 32.0, "the line never reached its guns")
    assert(hull.hp < hull.maxHp, "the line closed but never opened fire")
  }
}
