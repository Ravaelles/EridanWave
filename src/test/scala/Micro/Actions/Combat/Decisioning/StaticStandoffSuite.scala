package Micro.Actions.Combat.Decisioning

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class StaticStandoffSuite extends AnyFunSuite {

  test("the stand radius follows the guns, not the map") {
    assert(StaticStandoff.standTiles(vsTerran = false, siegedTanks = false, vsProtoss = true) == 7.8)
    assert(StaticStandoff.standTiles(vsTerran = false, siegedTanks = false, vsProtoss = false) == 9.1)
    assert(StaticStandoff.standTiles(vsTerran = false, siegedTanks = true, vsProtoss = false) == 9.1)
    assert(StaticStandoff.standTiles(vsTerran = true, siegedTanks = false, vsProtoss = false) == 6.6)
    assert(StaticStandoff.standTiles(vsTerran = true, siegedTanks = true, vsProtoss = false) == 15.2)
    assert(StaticStandoff.standTiles(vsTerran = true, siegedTanks = true, vsProtoss = true) == 15.2)
  }

  test("inside the stand radius the unit holds instead of closing") {
    assert(StaticStandoff.shouldHold(7.8, standTiles = 7.8, backoffTiles = 0.0))
    assert(StaticStandoff.shouldHold(5.0, standTiles = 7.8, backoffTiles = 0.0))
    assert( ! StaticStandoff.shouldHold(7.81, standTiles = 7.8, backoffTiles = 0.0))
  }

  test("wounded hulls hold the line further out") {
    assert(StaticStandoff.shouldHold(10.5, standTiles = 7.8, backoffTiles = 2.7))
    assert( ! StaticStandoff.shouldHold(10.51, standTiles = 7.8, backoffTiles = 2.7))
    assert( ! StaticStandoff.shouldHold(7.81, standTiles = 7.8, backoffTiles = 0.0))
  }

  test("inside the guns the unit eases out instead of standing") {
    assert(StaticStandoff.shouldNudge(distTiles = 4.0, staticRangeTiles = 4.0, backoffTiles = 0.0))
    assert(StaticStandoff.shouldNudge(distTiles = 3.9, staticRangeTiles = 4.0, backoffTiles = 0.0))
    assert( ! StaticStandoff.shouldNudge(distTiles = 4.1, staticRangeTiles = 4.0, backoffTiles = 0.0))
  }

  test("wounded hulls ease out before the guns reach them") {
    assert(StaticStandoff.shouldNudge(distTiles = 6.7, staticRangeTiles = 4.0, backoffTiles = 2.7))
    assert( ! StaticStandoff.shouldNudge(distTiles = 6.71, staticRangeTiles = 4.0, backoffTiles = 2.7))
  }

  test("the nudge steps a few pixels straight off the static") {
    val back = StaticStandoff.backoff(Pixel(0, 0), Pixel(96, 0))
    assert(back.x < 0 && back.y == 0)
    assert(Math.abs(back.x) <= 2)
  }
}
