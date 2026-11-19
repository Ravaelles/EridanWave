package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class LoneMarchSuite extends AnyFunSuite {

  test("a warrior within fifteen tiles still forms up") {
    assert( ! LoneMarch.shouldMarchAlone(0.0))
    assert( ! LoneMarch.shouldMarchAlone(14.99))
    assert( ! LoneMarch.shouldMarchAlone(15.0))
  }

  test("past fifteen tiles with no warrior nearby the unit marches alone") {
    assert(LoneMarch.shouldMarchAlone(15.01))
    assert(LoneMarch.shouldMarchAlone(400.0))
  }

  test("the latch marches alone past 17 and rejoins below 13") {
    LoneMarch.reset()
    assert( ! LoneMarch.marchAlone(1, 0.0))
    assert(LoneMarch.marchAlone(1, 17.01))
    // Inside the band it holds its side instead of flapping.
    assert(LoneMarch.marchAlone(1, 15.0))
    assert(LoneMarch.marchAlone(1, 14.0))
    assert( ! LoneMarch.marchAlone(1, 12.99))
    assert( ! LoneMarch.marchAlone(1, 15.0))
    assert( ! LoneMarch.marchAlone(1, 16.0))
    assert(LoneMarch.marchAlone(1, 17.01))
  }

  test("the latch is per-unit and reset clears it") {
    LoneMarch.reset()
    assert(LoneMarch.marchAlone(1, 99.0))
    assert( ! LoneMarch.marchAlone(2, 0.0))
    LoneMarch.reset()
    assert( ! LoneMarch.marchAlone(1, 15.0))
  }
}
