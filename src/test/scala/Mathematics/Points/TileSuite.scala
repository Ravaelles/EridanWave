package Mathematics.Points

import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class TileSuite extends AnyFunSuite {

  test("index folds row-major on the seeded map") {
    TestKit.seedWorld(mapTiles = 128)
    assert(Tile(0, 0).i == 0)
    assert(Tile(5, 3).i == 5 + 3 * 128)
  }

  test("validity follows the seeded dims") {
    TestKit.seedWorld(mapTiles = 64)
    assert(Tile(0, 0).valid)
    assert(Tile(63, 63).valid)
    assert( ! Tile(64, 0).valid)
    assert( ! Tile(-1, 10).valid)
  }

  test("clip pins to the seeded dims") {
    TestKit.seedWorld(mapTiles = 64)
    assert(Tile(99, -4).clip == Tile(63, 0))
  }

  test("add moves by deltas") {
    TestKit.seedWorld()
    assert(Tile(10, 10).add(3, -4) == Tile(13, 6))
  }
}
