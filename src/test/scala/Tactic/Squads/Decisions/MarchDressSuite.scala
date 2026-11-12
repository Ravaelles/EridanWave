package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class MarchDressSuite extends AnyFunSuite {

  test("leader ahead of the pack mean waits") {
    assert(MarchDress.shouldWait(ownDistToGoalTiles = 8.0, meanDistToGoalTiles = 11.0))
  }

  test("units within tolerance of the mean march on") {
    assert( ! MarchDress.shouldWait(ownDistToGoalTiles = 9.0, meanDistToGoalTiles = 11.0))
    assert( ! MarchDress.shouldWait(ownDistToGoalTiles = 11.0, meanDistToGoalTiles = 11.0))
  }

  test("laggards behind the mean march on") {
    assert( ! MarchDress.shouldWait(ownDistToGoalTiles = 14.0, meanDistToGoalTiles = 11.0))
  }

  test("boundary is strict: exactly at tolerance marches, past it waits") {
    assert( ! MarchDress.shouldWait(ownDistToGoalTiles = 9.0, meanDistToGoalTiles = 9.0 + MarchDress.waitAheadTiles))
    assert(MarchDress.shouldWait(ownDistToGoalTiles = 9.0 - 0.01, meanDistToGoalTiles = 9.0 + MarchDress.waitAheadTiles))
  }

  test("no enemy inside twenty tiles means no dressing") {
    assert( ! MarchDress.enemyNear(20.01))
    assert( ! MarchDress.enemyNear(200.0))
    assert(MarchDress.enemyNear(20.0))
    assert(MarchDress.enemyNear(0.0))
  }

  test("an armed enemy in sight always dresses") {
    assert( ! MarchDress.shouldSkipWait(1000.0, 0.0, armedEnemyVisible = true))
  }

  test("a weak or strung-out pack dresses even unopposed") {
    assert( ! MarchDress.shouldSkipWait(149.0, 1.0, armedEnemyVisible = false))
    assert( ! MarchDress.shouldSkipWait(500.0, 3.01, armedEnemyVisible = false))
  }

  test("a strong compact pack marches through instead of dressing") {
    assert(MarchDress.shouldSkipWait(150.0, 3.0, armedEnemyVisible = false))
    assert(MarchDress.shouldSkipWait(900.0, 0.5, armedEnemyVisible = false))
  }
}
