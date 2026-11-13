package Micro.Targeting.FiltersRequired

import org.scalatest.funsuite.AnyFunSuite

class TargetFilterFocusSuite extends AnyFunSuite {

  test("cooling guns never wall-bust") {
    assert( ! TargetFilterFocus.wallBustLegal(coolingDown = true, noEnemyInsideMargin = true, targetIsBuilding = true, targetOutrangesUs = false))
  }

  test("an enemy inside the shot margin vetoes the wall-bust") {
    assert( ! TargetFilterFocus.wallBustLegal(coolingDown = false, noEnemyInsideMargin = false, targetIsBuilding = true, targetOutrangesUs = false))
  }

  test("non-buildings never wall-bust") {
    assert( ! TargetFilterFocus.wallBustLegal(coolingDown = false, noEnemyInsideMargin = true, targetIsBuilding = false, targetOutrangesUs = false))
  }

  test("idle guns with a clear foreground break blockers") {
    assert(TargetFilterFocus.wallBustLegal(coolingDown = false, noEnemyInsideMargin = true, targetIsBuilding = true, targetOutrangesUs = false))
  }

  test("outranging statics are never rushed") {
    assert( ! TargetFilterFocus.wallBustLegal(coolingDown = false, noEnemyInsideMargin = true, targetIsBuilding = true, targetOutrangesUs = true))
  }

  test("a building at our feet is always legal, assigned or not") {
    assert(TargetFilterFocus.pointBlankLegal(targetIsBuilding = true, targetOutrangesUs = false, distPixels = 0.0, ownRangePixels = 16.0))
    assert(TargetFilterFocus.pointBlankLegal(targetIsBuilding = true, targetOutrangesUs = false, distPixels = 48.0, ownRangePixels = 16.0))
    assert( ! TargetFilterFocus.pointBlankLegal(targetIsBuilding = true, targetOutrangesUs = false, distPixels = 49.0, ownRangePixels = 16.0))
  }

  test("point-blank never forces a suicide into outranging statics") {
    assert( ! TargetFilterFocus.pointBlankLegal(targetIsBuilding = true, targetOutrangesUs = true, distPixels = 0.0, ownRangePixels = 16.0))
    assert( ! TargetFilterFocus.pointBlankLegal(targetIsBuilding = false, targetOutrangesUs = false, distPixels = 0.0, ownRangePixels = 16.0))
  }

  test("the wall-bust margin is one and a half tiles") {
    assert(TargetFilterFocus.wallBustMarginTiles == 1.5)
  }
}
