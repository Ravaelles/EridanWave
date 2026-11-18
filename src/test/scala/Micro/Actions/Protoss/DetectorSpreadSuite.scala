package Micro.Actions.Protoss.Detector

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class DetectorSpreadSuite extends AnyFunSuite {

  test("observers keep six tiles off enemy detectors") {
    assert(DetectorSpread.shouldYieldToDetector(5.9))
    assert( ! DetectorSpread.shouldYieldToDetector(6.0))
    assert(DetectorSpread.standOff(Pixel(0, 0), Pixel(96, 0)) == Pixel(-96, 0))
  }

  test("the standoff latch enters sharp and releases past the margin") {
    DetectorSpread.reset()
    assert(DetectorSpread.standOffLatched(1, 5.9))
    assert(DetectorSpread.standOffLatched(1, 6.1))
    assert(DetectorSpread.standOffLatched(1, 7.9))
    assert( ! DetectorSpread.standOffLatched(1, 8.1))
    assert( ! DetectorSpread.standOffLatched(1, 6.1))
  }

  test("a fresh eye only yields past the sharp edge") {
    DetectorSpread.reset()
    assert(DetectorSpread.standOffLatched(7, 5.9))
    assert( ! DetectorSpread.standOffLatched(8, 6.1))
  }

  test("vessels keep eleven tiles off, latched like the generic ring") {
    assert(DetectorSpread.shouldYieldToVessel(10.9))
    assert( ! DetectorSpread.shouldYieldToVessel(11.0))
    DetectorSpread.reset()
    assert(DetectorSpread.standOffVesselLatched(1, 10.9))
    assert(DetectorSpread.standOffVesselLatched(1, 11.5))
    assert(DetectorSpread.standOffVesselLatched(1, 12.9))
    assert( ! DetectorSpread.standOffVesselLatched(1, 13.1))
    assert( ! DetectorSpread.standOffVesselLatched(1, 11.5))
  }

  test("a detected eye steps four off the gun centroid, landing past the line") {
    assert(DetectorSpread.shouldYieldToCentroid(3.9))
    assert( ! DetectorSpread.shouldYieldToCentroid(4.0))
    assert(DetectorSpread.stepOutFromCentroid(Pixel(0, 0), Pixel(96, 0)) == Pixel(-64, 0))
  }
}
