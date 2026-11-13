package Micro.Targeting.FiltersRequired

import org.scalatest.funsuite.AnyFunSuite

class TargetFilterWallBustSuite extends AnyFunSuite {

  test("cooling guns never wall-bust") {
    assert( ! TargetFilterWallBust.wallBustLegal(coolingDown = true, noEnemyInsideMargin = true, targetIsBuilding = true, targetOutrangesUs = false))
  }

  test("an enemy inside the shot margin vetoes the wall-bust") {
    assert( ! TargetFilterWallBust.wallBustLegal(coolingDown = false, noEnemyInsideMargin = false, targetIsBuilding = true, targetOutrangesUs = false))
  }

  test("non-buildings never wall-bust") {
    assert( ! TargetFilterWallBust.wallBustLegal(coolingDown = false, noEnemyInsideMargin = true, targetIsBuilding = false, targetOutrangesUs = false))
  }

  test("idle guns with a clear foreground break blockers") {
    assert(TargetFilterWallBust.wallBustLegal(coolingDown = false, noEnemyInsideMargin = true, targetIsBuilding = true, targetOutrangesUs = false))
  }

  test("outranging statics are never rushed") {
    assert( ! TargetFilterWallBust.wallBustLegal(coolingDown = false, noEnemyInsideMargin = true, targetIsBuilding = true, targetOutrangesUs = true))
  }

  test("the wall-bust margin is one and a half tiles") {
    assert(TargetFilterWallBust.wallBustMarginTiles == 1.5)
  }
}
