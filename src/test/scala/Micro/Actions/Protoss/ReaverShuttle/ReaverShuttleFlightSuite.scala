package Micro.Actions.Protoss.ReaverShuttle

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class ReaverShuttleFlightSuite extends AnyFunSuite {

  test("fresh Shuttles keep the flat margin") {
    assert(ReaverShuttleFlight.safetyMarginPixels(0.0, fasterFlyer = true) == 64)
    assert(ReaverShuttleFlight.safetyMarginPixels(0.0, fasterFlyer = false) == 160)
  }

  test("one wound curve prices the margin, nine tiles at full wounds") {
    assert(ReaverShuttleFlight.woundFleeBonusTiles == 9.0)
    assert(ReaverShuttleFlight.woundBonusTiles(0.5) == 4.5)
    assert(ReaverShuttleFlight.safetyMarginPixels(0.5, fasterFlyer = true) == 208)
    assert(ReaverShuttleFlight.safetyMarginPixels(1.0, fasterFlyer = false) == 448)
    assert(ReaverShuttleFlight.safetyMarginPixels(9.0, fasterFlyer = false) == 448)
  }

  test("melee reach never triggers the preemptive flee") {
    assert( ! ReaverShuttleFlight.threatensPreemptively(0.0, 32.0, 9.0 * 32.0))
    assert(ReaverShuttleFlight.threatensPreemptively(100.0, 128.0, 9.0 * 32.0))
    assert( ! ReaverShuttleFlight.threatensPreemptively(500.0, 128.0, 9.0 * 32.0))
  }

  test("the rear is the third of the cluster nearest our base") {
    val members = Seq(Pixel(320, 0), Pixel(640, 0), Pixel(960, 0), Pixel(9600, 0))
    val rear = ReaverShuttleFlight.rearOfCluster(members, Pixel(0, 0))
    assert(rear.x < 960 && rear.y == 0)
  }
}
