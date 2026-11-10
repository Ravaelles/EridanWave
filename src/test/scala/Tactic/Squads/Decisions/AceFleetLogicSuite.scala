package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class AceFleetLogicSuite extends AnyFunSuite {

  test("six units are a fleet against anything small") {
    assert(AceFleetLogic.hasFleet(ours = 6, weSplash = false, enemySplashes = false, enemies = 5))
    assert( ! AceFleetLogic.hasFleet(ours = 5, weSplash = false, enemySplashes = false, enemies = 5))
  }

  test("unopposed splash holds the floor of six") {
    assert(AceFleetLogic.hasFleet(ours = 6, weSplash = true, enemySplashes = false, enemies = 9))
    assert( ! AceFleetLogic.hasFleet(ours = 5, weSplash = true, enemySplashes = false, enemies = 9))
  }

  test("opposed splash counts the enemy above the floor") {
    assert(AceFleetLogic.hasFleet(ours = 6, weSplash = true, enemySplashes = true, enemies = 4))
    assert(AceFleetLogic.hasFleet(ours = 9, weSplash = false, enemySplashes = false, enemies = 9))
    assert( ! AceFleetLogic.hasFleet(ours = 8, weSplash = false, enemySplashes = false, enemies = 9))
  }
}
