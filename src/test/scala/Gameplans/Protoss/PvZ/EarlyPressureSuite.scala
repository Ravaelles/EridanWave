package Gameplans.Protoss.PvZ.policy

import org.scalatest.funsuite.AnyFunSuite

class EarlyPressureSuite extends AnyFunSuite {

  test("two complete warriors stay home") {
    assert( ! EarlyPressure.mayPush(0))
    assert( ! EarlyPressure.mayPush(1))
    assert( ! EarlyPressure.mayPush(2))
  }

  test("three complete warriors may push") {
    assert(EarlyPressure.mayPush(3))
    assert(EarlyPressure.mayPush(4))
    assert(EarlyPressure.mayPush(20))
  }
}
