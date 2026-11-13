package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class LoneMarchSuite extends AnyFunSuite {

  test("a warrior within ten tiles still forms up") {
    assert( ! LoneMarch.shouldMarchAlone(0.0))
    assert( ! LoneMarch.shouldMarchAlone(9.99))
    assert( ! LoneMarch.shouldMarchAlone(10.0))
  }

  test("past ten tiles with no warrior nearby the unit marches alone") {
    assert(LoneMarch.shouldMarchAlone(10.01))
    assert(LoneMarch.shouldMarchAlone(400.0))
  }
}
