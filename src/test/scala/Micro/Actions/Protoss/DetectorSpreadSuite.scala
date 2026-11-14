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
}
