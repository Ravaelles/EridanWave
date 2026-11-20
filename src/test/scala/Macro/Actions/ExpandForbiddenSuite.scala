package Macro.Actions

import org.scalatest.funsuite.AnyFunSuite

class ExpandForbiddenSuite extends AnyFunSuite {

  test("past twelve minutes the base is unconditional") {
    assert( ! ExpandForbidden.check(minutes = 12.0, armyStrengthUnits = 0.0, enemyStronger = true, darkTemplarShown = true, detectionReady = false, forgeAffordable = false))
    assert( ! ExpandForbidden.shouldCancelBase(minutes = 12.0, morphing = true, darkTemplarShown = true, detectionReady = false, forgeAffordable = false))
  }

  test("ten minutes with the backbone earn the base") {
    assert( ! ExpandForbidden.check(minutes = 10.0, armyStrengthUnits = 160.0, enemyStronger = true, darkTemplarShown = true, detectionReady = false, forgeAffordable = false))
    assert(ExpandForbidden.check(minutes = 10.0, armyStrengthUnits = 159.9, enemyStronger = true, darkTemplarShown = false, detectionReady = false, forgeAffordable = false))
  }

  test("a stronger enemy forbids new bases early") {
    assert(ExpandForbidden.check(minutes = 5.0, armyStrengthUnits = 50.0, enemyStronger = true, darkTemplarShown = false, detectionReady = false, forgeAffordable = true))
    assert( ! ExpandForbidden.check(minutes = 5.0, armyStrengthUnits = 50.0, enemyStronger = false, darkTemplarShown = false, detectionReady = false, forgeAffordable = true))
  }

  test("an unanswered blade with no Forge money forbids and cancels") {
    assert(ExpandForbidden.check(minutes = 5.0, armyStrengthUnits = 50.0, enemyStronger = false, darkTemplarShown = true, detectionReady = false, forgeAffordable = false))
    assert(ExpandForbidden.shouldCancelBase(minutes = 5.0, morphing = true, darkTemplarShown = true, detectionReady = false, forgeAffordable = false))
  }

  test("answered blades and affordable Forges never forbid") {
    assert( ! ExpandForbidden.check(minutes = 5.0, armyStrengthUnits = 50.0, enemyStronger = false, darkTemplarShown = true, detectionReady = true, forgeAffordable = false))
    assert( ! ExpandForbidden.check(minutes = 5.0, armyStrengthUnits = 50.0, enemyStronger = false, darkTemplarShown = true, detectionReady = false, forgeAffordable = true))
    assert( ! ExpandForbidden.shouldCancelBase(minutes = 5.0, morphing = true, darkTemplarShown = true, detectionReady = true, forgeAffordable = false))
  }

  test("nothing morphing means nothing to cancel") {
    assert( ! ExpandForbidden.shouldCancelBase(minutes = 5.0, morphing = false, darkTemplarShown = true, detectionReady = false, forgeAffordable = false))
  }
}
