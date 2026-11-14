package Micro.Actions.Protoss.ReaverShuttle

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class ReaverShuttleFlightSuite extends AnyFunSuite {

  test("fresh Shuttles keep the flat margin") {
    assert(ReaverShuttleFlight.safetyMarginPixels(0.0, fasterFlyer = true) == 64)
    assert(ReaverShuttleFlight.safetyMarginPixels(0.0, fasterFlyer = false) == 160)
  }

  test("one wound curve prices the margin, nine tiles at full wounds") {
    assert(ReaverShuttleFlight.woundBonusTiles(0.5) == 4.5)
    assert(ReaverShuttleFlight.safetyMarginPixels(0.5, fasterFlyer = true) == 208)
    assert(ReaverShuttleFlight.safetyMarginPixels(1.0, fasterFlyer = false) == 448)
  }

  test("penetration is how deep a threat sits inside its trigger") {
    assert(ReaverShuttleFlight.penetrationPixels(distPixels = 100.0, triggerPixels = 480.0) == 380.0)
    assert(ReaverShuttleFlight.penetrationPixels(distPixels = 500.0, triggerPixels = 480.0) == -20.0)
  }

  test("the rear is the third of the cluster nearest our base") {
    val members = Seq(Pixel(320, 0), Pixel(640, 0), Pixel(960, 0), Pixel(9600, 0))
    val rear = ReaverShuttleFlight.rearOfCluster(members, Pixel(0, 0))
    assert(rear.x < 960 && rear.y == 0)
  }

  test("the worst penetration is the deepest trigger") {
    assert(ReaverShuttleFlight.worstPenetration(Seq.empty) == Double.NegativeInfinity)
    assert(ReaverShuttleFlight.worstPenetration(Seq(-100.0, -10.0)) == -10.0)
    assert(ReaverShuttleFlight.worstPenetration(Seq(-100.0, 5.0)) == 5.0)
  }

  test("the flee latch enters sharp and releases two tiles past every trigger") {
    ReaverShuttleFlight.reset()
    assert(ReaverShuttleFlight.fleeLatched(1, 1.0))
    assert(ReaverShuttleFlight.fleeLatched(1, -1.0))
    assert(ReaverShuttleFlight.fleeLatched(1, -63.0))
    assert( ! ReaverShuttleFlight.fleeLatched(1, -65.0))
    assert( ! ReaverShuttleFlight.fleeLatched(1, -1.0))
  }

  test("a fresh hull only flees past a live trigger") {
    ReaverShuttleFlight.reset()
    assert(ReaverShuttleFlight.fleeLatched(7, 1.0))
    assert( ! ReaverShuttleFlight.fleeLatched(8, -1.0))
    assert( ! ReaverShuttleFlight.fleeLatched(8, Double.NegativeInfinity))
  }
}
