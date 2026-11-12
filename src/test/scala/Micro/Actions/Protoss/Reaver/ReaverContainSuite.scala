package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverContainSuite extends AnyFunSuite {

  test("a lone warrior against one static is a contain") {
    assert(ReaverContain.containing(ourWarriors = 1, enemyStaticsNear = 1))
    assert(ReaverContain.containing(ourWarriors = 11, enemyStaticsNear = 3))
    assert(ReaverContain.containing(ourWarriors = 50, enemyStaticsNear = 9))
  }

  test("nothing on either side is not a contain") {
    assert( ! ReaverContain.containing(ourWarriors = 0, enemyStaticsNear = 1))
    assert( ! ReaverContain.containing(ourWarriors = 1, enemyStaticsNear = 0))
    assert( ! ReaverContain.containing(ourWarriors = 0, enemyStaticsNear = 0))
  }

  test("the hold releases only on collapse") {
    assert(ReaverContain.holding(currentlyHolding = false, ourWarriors = 1, enemyStaticsNear = 1))
    assert( ! ReaverContain.holding(currentlyHolding = false, ourWarriors = 0, enemyStaticsNear = 1))
    assert( ! ReaverContain.holding(currentlyHolding = false, ourWarriors = 1, enemyStaticsNear = 0))
    assert(ReaverContain.holding(currentlyHolding = true, ourWarriors = 1, enemyStaticsNear = 1))
    assert( ! ReaverContain.holding(currentlyHolding = true, ourWarriors = 0, enemyStaticsNear = 1))
    assert( ! ReaverContain.holding(currentlyHolding = true, ourWarriors = 1, enemyStaticsNear = 0))
    assert( ! ReaverContain.holding(currentlyHolding = true, ourWarriors = 0, enemyStaticsNear = 0))
  }

  test("only Zerg recalls every Reaver") {
    assert(ReaverContain.recallAll(isZerg = true))
    assert( ! ReaverContain.recallAll(isZerg = false))
  }

  test("thresholds are stored in tiles") {
    assert(ReaverContain.staticsDistTiles == 25.0)
    assert(ReaverContain.fightDistTiles == 15.0)
  }
}
