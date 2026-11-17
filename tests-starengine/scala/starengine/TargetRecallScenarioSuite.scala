package starengine

import Micro.Targeting.TargetRecall
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Remembered-target recall, step by step: a Dragoon stands 350px from a
 * tracked Pylon it cannot see (fog, or a choke away - either way outside
 * both the live sources). The old brain only answers seen foes and holds
 * until timeout while the base it marched on stands next door. The fixed
 * brain walks remembered fixes inside the shipped twelve-tile horizon
 * and shoots on arrival; past the horizon both brains hold, so no hull
 * ever commutes map-wide at a ghost. Speeds, reaches and damage are the
 * real UnitType numbers; the fixed pilot calls the shipped horizon
 * kernel, the engine owns the loop only.
 */
class TargetRecallScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Targeting.name + StarEngineTags.Hunt.name) }

  /** Pilot, production order stripped to the duel: seen foes, then the remembered fix. */
  private class RecallPilot(memory: Option[(Double, Double)], useRecall: Boolean) extends Pilot {
    var shots = 0

    override def direct(me: Body, world: World): Directive = {
      val seen = world.foes.filter(f => f.alive && me.dist(f) <= me.kind.sightPixels)
      if (seen.nonEmpty) {
        val prey = seen.minBy(me.dist)
        shots += 1
        return Chase(prey)
      }
      memory match {
        case Some((gx, gy)) if useRecall && TargetRecall.shouldRecall(Math.hypot(me.x - gx, me.y - gy) / 32.0) =>
          shots += 1
          MoveTo(gx, gy)
        case _ => Hold
      }
    }
  }

  private case class Outcome(dragoonX: Double, dragoonY: Double, pylonHp: Double, pylonAlive: Boolean, shots: Int)

  private def runRecall(pylonDistPx: Double, useRecall: Boolean, frames: Int): Outcome = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 0.0, Protoss.Dragoon.maxTotalHealth)
    val pylon = new Body(Protoss.Pylon, 0.0, pylonDistPx, Protoss.Pylon.maxTotalHealth)
    val pilot = new RecallPilot(Some((0.0, pylonDistPx)), useRecall)
    val world = new World(Seq(dragoon), Seq(pylon),
      Map(dragoon -> pilot, pylon -> new Pilots.Hold), Seq.empty)
    var f = 0
    while (f < frames && dragoon.alive && pylon.alive) { world.step(); f += 1 }
    Outcome(dragoon.x, dragoon.y, pylon.hp, pylon.alive, pilot.shots)
  }

  test("a remembered base inside the horizon is walked down and razed") {
    val fixed = runRecall(pylonDistPx = 350.0, useRecall = true, frames = 1200)
    println(s"DIAG fixed pylon=${fixed.pylonHp.toInt} shots=${fixed.shots}")
    assert( ! fixed.pylonAlive, "the dragoon never walked 350px to the remembered base")
    assert(fixed.shots > 0, "no hunt orders issued at a remembered fix")

    val old = runRecall(pylonDistPx = 350.0, useRecall = false, frames = 1200)
    println(s"DIAG old pylon=${old.pylonHp.toInt} shots=${old.shots}")
    assert(old.pylonAlive && old.pylonHp == Protoss.Pylon.maxTotalHealth, "the control found the base on its own - nothing to fix here")
    assert(Math.hypot(old.dragoonX, old.dragoonY) < 32.0, "the control left without a target")
  }

  test("a remembered base past the horizon is left to the march") {
    val fixed = runRecall(pylonDistPx = 500.0, useRecall = true, frames = 600)
    println(s"DIAG far pylon=${fixed.pylonHp.toInt} shots=${fixed.shots}")
    assert(fixed.pylonAlive, "the dragoon commuted map-wide at a ghost")
    assert(Math.hypot(fixed.dragoonX, fixed.dragoonY) < 32.0, "the dragoon left its post for a far memory")
    assert(fixed.shots == 0, "hunt orders issued past the horizon")
  }
}
