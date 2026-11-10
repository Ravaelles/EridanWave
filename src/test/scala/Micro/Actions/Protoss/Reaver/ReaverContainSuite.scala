package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverContainSuite extends AnyFunSuite {

  test("eleven warriors against three statics is a contain") {
    assert(ReaverContain.containing(ourWarriors = 11, enemyStaticsNear = 3))
    assert(ReaverContain.containing(ourWarriors = 50, enemyStaticsNear = 9))
  }

  test("ten warriors is a raid, not a contain") {
    assert( ! ReaverContain.containing(ourWarriors = 10, enemyStaticsNear = 3))
    assert( ! ReaverContain.containing(ourWarriors = 11, enemyStaticsNear = 2))
    assert( ! ReaverContain.containing(ourWarriors = 0, enemyStaticsNear = 0))
  }

  test("only Zerg recalls every Reaver") {
    assert(ReaverContain.recallAll(isZerg = true))
    assert( ! ReaverContain.recallAll(isZerg = false))
  }

  test("thresholds are stored in tiles") {
    assert(ReaverContain.staticsDistTiles == 20.0)
    assert(ReaverContain.fightDistTiles == 15.0)
  }
}
