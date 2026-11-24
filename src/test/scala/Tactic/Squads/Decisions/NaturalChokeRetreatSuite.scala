package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class NaturalChokeRetreatSuite extends AnyFunSuite {

  test("a handful facing Zerg inside the score falls back to the natural") {
    assert(NaturalChokeRetreat.maxWarriors == 10)
    assert(NaturalChokeRetreat.weakArmyUnits == 200.0)
    assert(NaturalChokeRetreat.foeNearTiles == 12.0)
    assert(NaturalChokeRetreat.shouldRetreat(enemyIsZerg = true, ourWarriors = 9, armyUnitsScore = 199.9, nearestArmedTiles = 12.0))
    assert(NaturalChokeRetreat.shouldRetreat(enemyIsZerg = true, ourWarriors = 0, armyUnitsScore = 0.0, nearestArmedTiles = 0.0))
  }

  test("strong lines, other matchups and empty fields march as before") {
    assert( ! NaturalChokeRetreat.shouldRetreat(enemyIsZerg = false, ourWarriors = 9, armyUnitsScore = 199.9, nearestArmedTiles = 12.0))
    assert( ! NaturalChokeRetreat.shouldRetreat(enemyIsZerg = true, ourWarriors = 10, armyUnitsScore = 199.9, nearestArmedTiles = 12.0))
    assert( ! NaturalChokeRetreat.shouldRetreat(enemyIsZerg = true, ourWarriors = 9, armyUnitsScore = 200.0, nearestArmedTiles = 12.0))
    assert( ! NaturalChokeRetreat.shouldRetreat(enemyIsZerg = true, ourWarriors = 9, armyUnitsScore = 199.9, nearestArmedTiles = 12.01))
  }
}
