package Tactic

import org.scalatest.funsuite.AnyFunSuite

class HomeGuardSuite extends AnyFunSuite {

  test("twenty-five warriors plus one harasser posts a guard") {
    assert(HomeGuard.shouldGuard(armyWarriors = 25, harassers = 1))
    assert(HomeGuard.shouldGuard(armyWarriors = 60, harassers = 4))
  }

  test("a small army never spares anyone") {
    assert( ! HomeGuard.shouldGuard(armyWarriors = 24, harassers = 3))
    assert( ! HomeGuard.shouldGuard(armyWarriors = 0, harassers = 1))
  }

  test("no harassers means no guard") {
    assert( ! HomeGuard.shouldGuard(armyWarriors = 40, harassers = 0))
  }
}
