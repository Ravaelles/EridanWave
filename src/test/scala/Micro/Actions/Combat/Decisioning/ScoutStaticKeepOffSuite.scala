package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class ScoutStaticKeepOffSuite extends AnyFunSuite {

  test("the hold sits on the static-target line at reach plus margin") {
    val hold = ScoutStaticKeepOff.holdPoint(
      staticX = 0.0, staticY = 0.0, staticRangePixels = 200.0,
      targetX = 400.0, targetY = 0.0,
      weaponRangePixels = 300.0)
    assert(hold.isDefined)
    assert(Math.abs(hold.get._1 - 232.0) < 1e-9)
    assert(Math.abs(hold.get._2 - 0.0) < 1e-9)
  }

  test("a target under the umbrella keeps the normal duel") {
    assert(ScoutStaticKeepOff.holdPoint(
      staticX = 0.0, staticY = 0.0, staticRangePixels = 200.0,
      targetX = 100.0, targetY = 0.0,
      weaponRangePixels = 100.0).isEmpty)
  }

  test("a coincident target never divides by zero") {
    assert(ScoutStaticKeepOff.holdPoint(
      staticX = 50.0, staticY = 50.0, staticRangePixels = 200.0,
      targetX = 50.0, targetY = 50.0,
      weaponRangePixels = 300.0).isEmpty)
  }
}
