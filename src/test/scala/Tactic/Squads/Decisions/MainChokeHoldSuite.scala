package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class MainChokeHoldSuite extends AnyFunSuite {

  test("a pack of four to eight against Zerg holds the main choke") {
    assert(MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 4, unitsScore = 160.0))
    assert(MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 5, unitsScore = 160.0))
    assert(MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 8, unitsScore = 160.0))
  }

  test("other races form up instead") {
    assert( ! MainChokeHold.shouldHold(enemyIsZerg = false, warriors = 5, unitsScore = 160.0))
    assert( ! MainChokeHold.shouldHold(enemyIsZerg = false, warriors = 4, unitsScore = 0.0))
  }

  test("a lone hull or a real army forms up instead") {
    assert( ! MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 0, unitsScore = 0.0))
    assert( ! MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 1, unitsScore = 40.0))
    assert( ! MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 3, unitsScore = 100.0))
    assert( ! MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 9, unitsScore = 100.0))
    assert( ! MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 5, unitsScore = 160.01))
  }

  test("only a healthy hull posted at the choke with a mate holds it") {
    assert(MainChokeHold.shouldHoldUnit(totalHp = 30, distToChokeTiles = 1.0, nearestMateTiles = 0.5))
    assert(MainChokeHold.shouldHoldUnit(totalHp = 200, distToChokeTiles = 0.0, nearestMateTiles = 0.0))
  }

  test("a dying hull never holds alone") {
    assert( ! MainChokeHold.shouldHoldUnit(totalHp = 29, distToChokeTiles = 0.0, nearestMateTiles = 0.0))
  }

  test("a hull off the choke never holds it") {
    assert( ! MainChokeHold.shouldHoldUnit(totalHp = 200, distToChokeTiles = 1.01, nearestMateTiles = 0.0))
  }

  test("a lone hull never holds it, however healthy and posted") {
    assert( ! MainChokeHold.shouldHoldUnit(totalHp = 200, distToChokeTiles = 0.0, nearestMateTiles = 0.51))
    assert( ! MainChokeHold.shouldHoldUnit(totalHp = 200, distToChokeTiles = 0.0, nearestMateTiles = Double.PositiveInfinity))
  }
}
