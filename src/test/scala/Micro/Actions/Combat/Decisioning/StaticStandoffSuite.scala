package Micro.Actions.Combat.Decisioning

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class StaticStandoffSuite extends AnyFunSuite {

  test("inside the stand radius the unit holds instead of closing") {
    assert(StaticStandoff.shouldHold(9.1))
    assert(StaticStandoff.shouldHold(5.0))
    assert( ! StaticStandoff.shouldHold(9.11))
  }

  test("inside the guns the unit eases out instead of standing") {
    assert(StaticStandoff.shouldNudge(distTiles = 4.0, staticRangeTiles = 4.0))
    assert(StaticStandoff.shouldNudge(distTiles = 3.9, staticRangeTiles = 4.0))
    assert( ! StaticStandoff.shouldNudge(distTiles = 4.1, staticRangeTiles = 4.0))
  }

  test("the nudge steps a few pixels straight off the static") {
    val back = StaticStandoff.backoff(Pixel(0, 0), Pixel(96, 0))
    assert(back.x < 0 && back.y == 0)
    assert(Math.abs(back.x) <= 2)
  }
}
