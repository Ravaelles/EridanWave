package Mathematics

import org.scalatest.funsuite.AnyFunSuite

class WoundMarginSuite extends AnyFunSuite {

  test("fresh hulls keep the low edge, dying hulls take the high edge") {
    val margin = WoundMargin(4.0, 13.0)
    assert(margin.tiles(0.0) == 4.0)
    assert(margin.tiles(1.0) == 13.0)
    assert(margin.tiles(0.5) == 8.5)
  }

  test("the lerp clamps outside zero to one") {
    val margin = WoundMargin(0.0, 8.0)
    assert(margin.tiles(-1.0) == 0.0)
    assert(margin.tiles(2.0) == 8.0)
  }

  test("pixels scale tiles by thirty-two") {
    val margin = WoundMargin(0.0, 10.0)
    assert(margin.pixels(0.0) == 0.0)
    assert(margin.pixels(1.0) == 320.0)
    assert(margin.pixels(0.5) == 160.0)
  }
}
