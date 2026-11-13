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

  test("the latch marches alone past 10.5 and rejoins below 9.5") {
    LoneMarch.reset()
    assert( ! LoneMarch.marchAlone(1, 0.0))
    assert(LoneMarch.marchAlone(1, 10.51))
    // Inside the band it holds its side instead of flapping.
    assert(LoneMarch.marchAlone(1, 10.0))
    assert(LoneMarch.marchAlone(1, 9.6))
    assert( ! LoneMarch.marchAlone(1, 9.49))
    assert( ! LoneMarch.marchAlone(1, 10.0))
    assert( ! LoneMarch.marchAlone(1, 10.4))
    assert(LoneMarch.marchAlone(1, 10.51))
  }

  test("the latch is per-unit and reset clears it") {
    LoneMarch.reset()
    assert(LoneMarch.marchAlone(1, 99.0))
    assert( ! LoneMarch.marchAlone(2, 0.0))
    LoneMarch.reset()
    assert( ! LoneMarch.marchAlone(1, 10.0))
  }
}
