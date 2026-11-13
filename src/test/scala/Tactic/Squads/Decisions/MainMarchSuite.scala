package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class MainMarchSuite extends AnyFunSuite {

  test("small armies still dress at home") {
    assert( ! MainMarch.shouldSkip(supplyUsed = 90, distToMainTiles = 0.0))
    assert( ! MainMarch.shouldSkip(supplyUsed = 12, distToMainTiles = 0.0))
  }

  test("far armies still dress on the march") {
    assert( ! MainMarch.shouldSkip(supplyUsed = 200, distToMainTiles = 35.01))
    assert( ! MainMarch.shouldSkip(supplyUsed = 200, distToMainTiles = 400.0))
  }

  test("a big army at home marches as a column") {
    assert(MainMarch.shouldSkip(supplyUsed = 91, distToMainTiles = 35.0))
    assert(MainMarch.shouldSkip(supplyUsed = 400, distToMainTiles = 0.0))
  }
}
