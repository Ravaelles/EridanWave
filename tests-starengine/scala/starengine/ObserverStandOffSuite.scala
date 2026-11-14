package starengine

import Micro.Actions.Protoss.Detector.DetectorSpread
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Observer standoff vs escort: the monitor pulls the eye toward the pack
 * centroid every frame while the brain pushes off enemy detectors inside
 * six tiles. A breathing centroid (±1.5 tiles, the pack repositioning)
 * crosses the sharp edge twice per breath, so the eye flips standoff and
 * escort hundreds of times and never settles - live games show 1400-2200
 * flips on one eye until hunters catch it. The latched eye enters sharp,
 * holds past the edge and releases two tiles out. Reaches and speeds are
 * the real UnitType numbers; both pilots call the shipped kernels.
 */
class ObserverStandOffSuite extends AnyFunSuite {

  private class EyePilot(id: Int, latched: Boolean) extends Pilot {
    val yielded = new scala.collection.mutable.ListBuffer[Boolean]()

    override def direct(me: Body, world: World): Directive = {
      val foe = world.foes.find(_.alive)
      foe match {
        case None => Hold
        case Some(f) =>
          val distTiles = me.dist(f) / 32.0
          val yieldNow =
            if (latched) DetectorSpread.standOffLatched(id, distTiles)
            else DetectorSpread.shouldYieldToDetector(distTiles)
          yielded += yieldNow
          if (yieldNow) {
            val dx = me.x - f.x
            val dy = me.y - f.y
            val dist = Math.sqrt(dx * dx + dy * dy)
            if (dist > 0) {
              val stand = DetectorSpread.enemyKeepOffTiles * 32.0
              MoveTo(f.x + dx / dist * stand, f.y + dy / dist * stand)
            } else Hold
          } else {
            MoveTo(400.0, anchorY(world.frame))
          }
      }
    }
  }

  private def anchorY(frame: Int): Double =
    300.0 + 6.0 * 32.0 + 48.0 * Math.sin(frame / 60.0)

  private case class Outcome(flips: Int, insideFrames: Int)

  private def runEye(latched: Boolean): Outcome = {
    if (latched) DetectorSpread.reset()
    val eye = new Body(Protoss.Observer, 400.0, 300.0 + 7.0 * 32.0, Protoss.Observer.maxTotalHealth)
    val over = new Body(Zerg.Overlord, 400.0, 300.0, Zerg.Overlord.maxTotalHealth)
    val pilot = new EyePilot(1, latched)
    val pilots: Map[Body, Pilot] = Map(eye -> pilot, over -> new Pilots.Hold)
    val world = new World(Seq(eye), Seq(over), pilots, Seq.empty)

    var inside = 0
    while (world.frame < 1500 && eye.alive) {
      world.step()
      if (eye.dist(over) / 32.0 < DetectorSpread.enemyKeepOffTiles) inside += 1
    }
    val flips = pilot.yielded.sliding(2).count(w => w(0) != w(1))
    Outcome(flips, inside)
  }

  test("a sharp edge flips escort and standoff every breath") {
    val eye = new Body(Protoss.Observer, 0.0, 0.0, 1.0)
    println(s"DIAG eye speed=${eye.speed}")
    val sharp = runEye(latched = false)
    println(s"DIAG sharp flips=${sharp.flips} inside=${sharp.insideFrames}f")
    assert(sharp.flips >= 6, s"no flap to fix: ${sharp.flips} flips")
  }

  test("a latched standoff holds through the breathing") {
    val fixed = runEye(latched = true)
    println(s"DIAG fixed flips=${fixed.flips} inside=${fixed.insideFrames}f")
    assert(fixed.flips <= 3, s"still flapping: ${fixed.flips} flips")
    // One entry frame is the sharp enter itself; the latch then holds out.
    assert(fixed.insideFrames <= 2, "latched eye keeps dipping inside")
  }
}
