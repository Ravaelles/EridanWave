package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class ScoutStaticKeepOffSuite extends AnyFunSuite {

  test("the hold sits on the static-target line at edge reach plus margin") {
    // A 2x2 static: the line starts at the box edge (32px out), not the
    // center - measuring from the center would donate the whole tile
    // of margin back to the guns.
    val hold = ScoutStaticKeepOff.holdPoint(
      staticX = 0.0, staticY = 0.0, staticTileWidth = 2, staticTileHeight = 2,
      staticRangePixels = 200.0,
      targetX = 400.0, targetY = 0.0,
      weaponRangePixels = 300.0)
    assert(hold.isDefined)
    assert(Math.abs(hold.get._1 - 264.0) < 1e-9)
    assert(Math.abs(hold.get._2 - 0.0) < 1e-9)
  }

  test("the diagonal hold clears the box corner, not the center") {
    val hold = ScoutStaticKeepOff.holdPoint(
      staticX = 0.0, staticY = 0.0, staticTileWidth = 2, staticTileHeight = 2,
      staticRangePixels = 200.0,
      targetX = 400.0, targetY = 400.0,
      weaponRangePixels = 300.0)
    assert(hold.isDefined)
    assert(Math.abs(hold.get._1 - 196.0) < 1.0)
    assert(Math.abs(hold.get._2 - 196.0) < 1.0)
  }

  test("a target under the umbrella keeps the normal duel") {
    assert(ScoutStaticKeepOff.holdPoint(
      staticX = 0.0, staticY = 0.0, staticTileWidth = 2, staticTileHeight = 2,
      staticRangePixels = 200.0,
      targetX = 100.0, targetY = 0.0,
      weaponRangePixels = 100.0).isEmpty)
  }

  test("a coincident target never divides by zero") {
    assert(ScoutStaticKeepOff.holdPoint(
      staticX = 50.0, staticY = 50.0, staticTileWidth = 2, staticTileHeight = 2,
      staticRangePixels = 200.0,
      targetX = 50.0, targetY = 50.0,
      weaponRangePixels = 300.0).isEmpty)
  }
}
