package starengine

import Micro.Actions.Combat.Decisioning.{ReengageLatch, TankHug}
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Shell shock, step by step: a Dragoon works a holding sieged tank. The
 * plain hunter AttackMoves into shell reach and dies there (the field
 * report: columns walking into guns behind a wall-in). The shocked pilot
 * calls the shipped TankHug gate: the first landed shell while outside
 * hugging range flips it into a retreat, and the shipped ReengageLatch
 * holds the retreat past the guns while the verdict still says fight.
 * Speeds, reaches and damage are the real UnitType numbers; both
 * decisions are shipped kernels.
 */
class ShellShockSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Retreat.name + StarEngineTags.Dragoon.name + StarEngineTags.Artillery.name) }

  /** Plain hunter: attack-move at the nearest foe, no matter the shells. */
  private class HuntPilot extends Pilot {

    override def direct(me: Body, world: World): Directive =
      world.nearestFoeOf(me, world.foes, (m, f) => m.canHit(f)) match {
        case Some(foe) => Chase(foe)
        case None      => Hold
      }
  }

  /**
   * Shocked pilot, production order: a landed shell outside hugging range
   * starts the retreat (the shipped TankHug gate on the pilot's own hit
   * clock), the shipped latch holds it past the guns, the verdict only
   * re-engages from a clear line.
   */
  private class ShockPilot extends Pilot {
    var withdrew = false
    var heldAgainstVerdict = 0
    private var lastHp = Double.NaN

    override def direct(me: Body, world: World): Directive = {
      if (lastHp.isNaN) lastHp = me.hp
      val shelled = me.hp < lastHp
      lastHp = me.hp
      val tank = world.foes.filter(f => f.alive && f.kind == Terran.SiegeTankSieged).sortBy(me.dist).headOption
      val inTankRange = tank.exists(t => me.dist(t) <= t.rangeAgainst(me))
      val shocked = inTankRange && shelled
      if (shocked) withdrew = true
      val entangled = tank.map(t => (me.dist(t) - t.rangeAgainst(me)) / 32.0).getOrElse(Double.NegativeInfinity)
      val verdict = inTankRange
      val hold = withdrew && ReengageLatch.shouldHoldRetreat(withdrew, entangled, verdict, 0.0)
      if (hold && verdict) heldAgainstVerdict += 1
      if (withdrew && hold) {
        tank match {
          case Some(t) =>
            val dx = me.x - t.x
            val dy = me.y - t.y
            val dist = Math.sqrt(dx * dx + dy * dy)
            if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)
            else Hold
          case None => Hold
        }
      } else {
        withdrew = false
        world.nearestFoeOf(me, world.foes, (m, f) => m.canHit(f)) match {
          case Some(foe) => Chase(foe)
          case None      => Hold
        }
      }
    }
  }

  private def runDuel(pilot: Pilot): (Body, Body, Int, Double) = {
    val dragoon = new Body(Protoss.Dragoon, 500.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val tank = new Body(Terran.SiegeTankSieged, 800.0, 500.0, Terran.SiegeTankSieged.maxTotalHealth)
    val world = new World(Seq(dragoon), Seq(tank), Map(dragoon -> pilot, tank -> new Pilots.Hold), Seq.empty)

    var maxDist = 0.0
    while (world.frame < 3000 && dragoon.alive) {
      world.step()
      maxDist = Math.max(maxDist, dragoon.dist(tank))
    }

    (dragoon, tank, world.frame, maxDist)
  }

  test("a plain hunter walks into shell reach and dies there") {
    val (dragoon, _, frames, _) = runDuel(new HuntPilot)
    println(s"DIAG hunt frames=$frames dragoon=${dragoon.hp.toInt}")
    assert( ! dragoon.alive, "hunter survived point-blank shells - no problem to fix")
  }

  test("the first shell starts a retreat that holds past the guns") {
    val pilot = new ShockPilot
    val (dragoon, tank, frames, maxDist) = runDuel(pilot)
    println(s"DIAG shock frames=$frames withdrew=${pilot.withdrew} held=${pilot.heldAgainstVerdict} dragoon=${dragoon.hp.toInt} maxDist=${maxDist.toInt}")
    assert(pilot.withdrew, "no retreat started on incoming shells")
    assert(pilot.heldAgainstVerdict > 0, "retreat never held against a live verdict")
    assert(dragoon.alive, "hull died withdrawing from the first shells")
    assert(maxDist > 384.0 + 160.0, f"hull never cleared the shell line: ${maxDist.toInt}px")
  }
}
