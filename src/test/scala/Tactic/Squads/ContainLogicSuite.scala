package Tactic.Squads

import org.scalatest.funsuite.AnyFunSuite

class ContainLogicSuite extends AnyFunSuite {

  test("a lone warrior against one static is a contain") {
    assert(ContainLogic.containing(ourWarriors = 1, enemyStaticsNear = 1))
    assert(ContainLogic.containing(ourWarriors = 11, enemyStaticsNear = 3))
    assert(ContainLogic.containing(ourWarriors = 50, enemyStaticsNear = 9))
  }

  test("nothing on either side is not a contain") {
    assert( ! ContainLogic.containing(ourWarriors = 0, enemyStaticsNear = 1))
    assert( ! ContainLogic.containing(ourWarriors = 1, enemyStaticsNear = 0))
    assert( ! ContainLogic.containing(ourWarriors = 0, enemyStaticsNear = 0))
  }

  test("the hold releases only on collapse") {
    assert(ContainLogic.holding(currentlyHolding = false, ourWarriors = 1, enemyStaticsNear = 1, chokeContained = false))
    assert( ! ContainLogic.holding(currentlyHolding = false, ourWarriors = 0, enemyStaticsNear = 1, chokeContained = false))
    assert( ! ContainLogic.holding(currentlyHolding = false, ourWarriors = 1, enemyStaticsNear = 0, chokeContained = false))
    assert(ContainLogic.holding(currentlyHolding = true, ourWarriors = 1, enemyStaticsNear = 1, chokeContained = false))
    assert( ! ContainLogic.holding(currentlyHolding = true, ourWarriors = 0, enemyStaticsNear = 1, chokeContained = false))
    assert( ! ContainLogic.holding(currentlyHolding = true, ourWarriors = 1, enemyStaticsNear = 0, chokeContained = false))
    assert( ! ContainLogic.holding(currentlyHolding = true, ourWarriors = 0, enemyStaticsNear = 0, chokeContained = false))
  }

  test("a held choke contain keeps holding on its own terms") {
    assert(ContainLogic.containingAtChoke(nearChoke = true, antiGroundStatics = 1, score = 0.0))
    assert(ContainLogic.containingAtChoke(nearChoke = true, antiGroundStatics = 3, score = 299.9))
    assert( ! ContainLogic.containingAtChoke(nearChoke = false, antiGroundStatics = 3, score = 0.0))
    assert( ! ContainLogic.containingAtChoke(nearChoke = true, antiGroundStatics = 0, score = 0.0))
    assert( ! ContainLogic.containingAtChoke(nearChoke = true, antiGroundStatics = 3, score = 300.0))
    assert(ContainLogic.holding(currentlyHolding = true, ourWarriors = 0, enemyStaticsNear = 0, chokeContained = true))
    assert(ContainLogic.holding(currentlyHolding = false, ourWarriors = 0, enemyStaticsNear = 0, chokeContained = true))
  }

  test("only Zerg recalls every Reaver") {
    assert(ContainLogic.recallAll(isZerg = true))
    assert( ! ContainLogic.recallAll(isZerg = false))
  }

  test("thresholds are stored in tiles") {
    assert(ContainLogic.staticsDistTiles == 25.0)
    assert(ContainLogic.fightDistTiles == 15.0)
    assert(ContainLogic.chokeContainTiles == 20.0)
    assert(ContainLogic.chokeContainMaxScore == 300.0)
  }
}
