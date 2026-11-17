package starengine

import Micro.Actions.Protoss.Dragoon.DragoonCarrierHunt
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Dragoon carrier steal: a Dragoon that would otherwise retreat shoots a
 * Carrier in range while its shields hold twenty, and closes a pathed
 * Carrier inside eleven tiles - to the hull with a ground path, else to
 * the halfway point with one, else not at all. The retreat verdict is
 * fixed for the duel (the steal owns it, never the fight); every pilot
 * calls the shipped kernels, the rects are the only terrain the engine
 * models. Shields are mapped from the pooled bar (full bar means full
 * shields - nothing shoots the Dragoon before it closes, so the mapping
 * never drifts mid-duel).
 */
class DragoonCarrierSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Carrier.name + StarEngineTags.Hunt.name) }

  private class CarrierPilot(carrier: Body, safety: (Double, Double)) extends Pilot {
    var minDist: Double = Double.PositiveInfinity

    override def direct(me: Body, world: World): Directive = {
      if ( ! carrier.alive) return MoveTo(safety._1, safety._2)
      minDist = Math.min(minDist, me.dist(carrier))
      val shields = (Protoss.Dragoon.maxShields * me.hp / me.maxHp).toInt
      val tiles = me.dist(carrier) / 32.0
      // Shoot: a shielded Dragoon always answers a Carrier in range.
      if (DragoonCarrierHunt.shouldShoot(isDragoon = true, shields, carrierInRange = me.dist(carrier) <= me.rangeAgainst(carrier))) {
        return Hold
      }
      // Close: to the hull with a path, else halfway with one, else not at all.
      val dist = me.dist(carrier)
      val toHull = world.groundPathExists(me.x, me.y, carrier.x, carrier.y)
      if ( ! DragoonCarrierHunt.shouldClose(isDragoon = true, shields, tiles, pathed = true)) {
        return MoveTo(safety._1, safety._2)
      }
      val (hx, hy) = ((me.x + carrier.x) / 2.0, (me.y + carrier.y) / 2.0)
      if (toHull) MoveTo(carrier.x, carrier.y)
      else if (dist > 0 && world.groundPathExists(me.x, me.y, hx, hy)) MoveTo(hx, hy)
      else MoveTo(safety._1, safety._2)
    }
  }

  private case class Outcome(minDist: Double, endX: Double, endY: Double, carrierHp: Double, carrierAlive: Boolean)

  private def runClose(rects: Seq[(Double, Double, Double, Double)]): Outcome = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, -300.0, Protoss.Dragoon.maxTotalHealth)
    val carrier = new Body(Protoss.Carrier, 0.0, 0.0, Protoss.Carrier.maxTotalHealth)
    carrier.passive = true
    val pilot = new CarrierPilot(carrier, (0.0, -600.0))
    val world = new World(Seq(dragoon), Seq(carrier),
      Map(dragoon -> pilot, carrier -> new Pilots.Hold), Seq.empty)
    world.blockedRects = rects
    var f = 0
    while (f < 400 && carrier.alive) { world.step(); f += 1 }
    Outcome(pilot.minDist, dragoon.x, dragoon.y, carrier.hp, carrier.alive)
  }

  test("an open Carrier is closed to gun range and answered") {
    val out = runClose(Seq.empty)
    println(s"DIAG open minDist=${out.minDist.toInt}px end=(${out.endX.toInt},${out.endY.toInt}) carrierHp=${out.carrierHp.toInt}")
    assert(out.minDist <= 128.0 + 32.0, "the Dragoon never closed the Carrier to its guns")
    assert(out.carrierHp < Protoss.Carrier.maxTotalHealth, "the Dragoon closed but never opened fire")
  }

  test("a sealed Carrier is held where the path stays open") {
    val out = runClose(Seq((-50.0, -120.0, 50.0, -20.0)))
    println(s"DIAG sealed minDist=${out.minDist.toInt}px end=(${out.endX.toInt},${out.endY.toInt})")
    assert(out.endY > -300.0, "the Dragoon never walked at the Carrier at all")
    assert(out.endY < -150.0, "the Dragoon walked past where its own halfway path closes")
    assert(out.minDist > 128.0, "the Dragoon reached gun range through the seal")
    assert(out.carrierHp == Protoss.Carrier.maxTotalHealth, "the Dragoon fired through the seal")
  }

  test("a Carrier with no path at all is not chased") {
    val out = runClose(Seq((-50.0, -200.0, 50.0, -100.0)))
    println(s"DIAG shut minDist=${out.minDist.toInt}px end=(${out.endX.toInt},${out.endY.toInt})")
    assert(Math.hypot(out.endX, out.endY + 600.0) < 64.0, "the Dragoon chased a Carrier it cannot reach")
  }
}
