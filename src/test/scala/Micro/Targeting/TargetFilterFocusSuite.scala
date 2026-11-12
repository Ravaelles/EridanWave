package Micro.Targeting.FiltersRequired

import org.scalatest.funsuite.AnyFunSuite

class TargetFilterFocusSuite extends AnyFunSuite {

  test("cooling guns never wall-bust") {
    assert( ! TargetFilterFocus.wallBustLegal(coolingDown = true, noEnemyInsideMargin = true, targetIsBuilding = true))
  }

  test("an enemy inside the shot margin vetoes the wall-bust") {
    assert( ! TargetFilterFocus.wallBustLegal(coolingDown = false, noEnemyInsideMargin = false, targetIsBuilding = true))
  }

  test("non-buildings never wall-bust") {
    assert( ! TargetFilterFocus.wallBustLegal(coolingDown = false, noEnemyInsideMargin = true, targetIsBuilding = false))
  }

  test("idle guns with a clear foreground break blockers") {
    assert(TargetFilterFocus.wallBustLegal(coolingDown = false, noEnemyInsideMargin = true, targetIsBuilding = true))
  }

  test("the wall-bust margin is one and a half tiles") {
    assert(TargetFilterFocus.wallBustMarginTiles == 1.5)
  }
}
