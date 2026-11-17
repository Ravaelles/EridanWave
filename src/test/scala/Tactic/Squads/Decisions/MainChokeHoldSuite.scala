package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class MainChokeHoldSuite extends AnyFunSuite {

  test("a handful against Zerg holds the main choke") {
    assert(MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 5, unitsScore = 160.0))
    assert(MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 0, unitsScore = 0.0))
    assert(MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 1, unitsScore = 40.0))
  }

  test("other races form up instead") {
    assert( ! MainChokeHold.shouldHold(enemyIsZerg = false, warriors = 5, unitsScore = 160.0))
    assert( ! MainChokeHold.shouldHold(enemyIsZerg = false, warriors = 0, unitsScore = 0.0))
  }

  test("a real army forms up instead") {
    assert( ! MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 6, unitsScore = 100.0))
    assert( ! MainChokeHold.shouldHold(enemyIsZerg = true, warriors = 5, unitsScore = 160.01))
  }
}
