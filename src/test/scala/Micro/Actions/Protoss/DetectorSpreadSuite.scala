package Micro.Actions.Protoss.Detector

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class DetectorSpreadSuite extends AnyFunSuite {

  test("a newer eye inside nine tiles moves off") {
    assert(DetectorSpread.shouldSpread(newerId = 2, olderId = 1, distTiles = 3.0))
    assert(DetectorSpread.shouldSpread(newerId = 2, olderId = 1, distTiles = 8.9))
  }

  test("the older eye holds its watch") {
    assert( ! DetectorSpread.shouldSpread(newerId = 1, olderId = 2, distTiles = 3.0))
  }

  test("nine tiles apart is far enough") {
    assert( ! DetectorSpread.shouldSpread(newerId = 2, olderId = 1, distTiles = 9.0))
    assert( ! DetectorSpread.shouldSpread(newerId = 2, olderId = 1, distTiles = 20.0))
  }

  test("the away point sits nine tiles past us") {
    assert(DetectorSpread.awayFrom(Pixel(0, 0), Pixel(320, 0)) == Pixel(32, 0))
  }

  test("the partner is the nearest older eye") {
    val olderNear = DetectorSpread.OlderEye(1, Pixel(0, 0), 3.0)
    val olderFar = DetectorSpread.OlderEye(2, Pixel(0, 0), 8.0)
    val newer = DetectorSpread.OlderEye(9, Pixel(0, 0), 1.0)
    assert(DetectorSpread.partner(5, Seq(olderFar, olderNear, newer)).contains(olderNear))
  }

  test("newer eyes and lone eyes have no partner") {
    val newer = DetectorSpread.OlderEye(9, Pixel(0, 0), 1.0)
    assert(DetectorSpread.partner(5, Seq(newer)).isEmpty)
    assert(DetectorSpread.partner(5, Seq.empty).isEmpty)
  }

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
