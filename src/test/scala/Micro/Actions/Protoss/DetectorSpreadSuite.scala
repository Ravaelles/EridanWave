package Micro.Actions.Protoss

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
}
