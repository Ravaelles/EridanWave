package Tactic.Missions

import org.scalatest.funsuite.AnyFunSuite

class ReaverDropAirSafetySuite extends AnyFunSuite {

  test("clear skies keep the independent cluster") {
    assert( ! ReaverDropAirSafety.mustJoinArmy(0, 3, 1, 2))
  }

  test("any Scourge grounds it") {
    assert(ReaverDropAirSafety.mustJoinArmy(1, 0, 0, 0))
  }

  test("four Mutalisks ground it, three do not") {
    assert(ReaverDropAirSafety.mustJoinArmy(0, 4, 0, 0))
    assert( ! ReaverDropAirSafety.mustJoinArmy(0, 3, 0, 0))
  }

  test("two Wraiths ground it, one does not") {
    assert(ReaverDropAirSafety.mustJoinArmy(0, 0, 2, 0))
    assert( ! ReaverDropAirSafety.mustJoinArmy(0, 0, 1, 0))
  }

  test("three Corsairs ground it, two do not") {
    assert(ReaverDropAirSafety.mustJoinArmy(0, 0, 0, 3))
    assert( ! ReaverDropAirSafety.mustJoinArmy(0, 0, 0, 2))
  }
}
