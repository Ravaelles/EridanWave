package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class RetreatExtraPenaltiesSuite extends AnyFunSuite {

  test("a withdrawal lasts two seconds whatever the forecast says") {
    assert(RetreatExtraPenalties.shouldHoldWithdraw(0))
    assert(RetreatExtraPenalties.shouldHoldWithdraw(47))
    assert( ! RetreatExtraPenalties.shouldHoldWithdraw(48))
    assert( ! RetreatExtraPenalties.shouldHoldWithdraw(1000))
  }

  test("fresh wounds read weakest, old ones read whole") {
    assert(RetreatExtraPenalties.advantageMultiplier(0) == 0.6)
    assert(RetreatExtraPenalties.advantageMultiplier(23) == 0.6)
    assert(RetreatExtraPenalties.advantageMultiplier(24) == 0.7)
    assert(RetreatExtraPenalties.advantageMultiplier(47) == 0.7)
    assert(RetreatExtraPenalties.advantageMultiplier(48) == 0.8)
    assert(RetreatExtraPenalties.advantageMultiplier(71) == 0.8)
    assert(RetreatExtraPenalties.advantageMultiplier(72) == 0.85)
    assert(RetreatExtraPenalties.advantageMultiplier(100) == 0.85)
    assert(RetreatExtraPenalties.advantageMultiplier(101) == 0.9)
    assert(RetreatExtraPenalties.advantageMultiplier(144) == 0.9)
    assert(RetreatExtraPenalties.advantageMultiplier(180) == 0.9)
    assert(RetreatExtraPenalties.advantageMultiplier(181) == 1.0)
    assert(RetreatExtraPenalties.advantageMultiplier(10000) == 1.0)
  }

  test("the floor matches the two-second minimum") {
    assert(RetreatExtraPenalties.minWithdrawFrames == 48)
  }

  test("a thin force deep out reads three quarters of its skim") {
    assert(RetreatExtraPenalties.penalizedScore(200.0, 8, 60.0) == 150.0)
    assert(RetreatExtraPenalties.penalizedScore(200.0, 11, 45.0) == 150.0)
  }

  test("full strength at home or in force reads whole") {
    assert(RetreatExtraPenalties.penalizedScore(200.0, 12, 60.0) == 200.0)
    assert(RetreatExtraPenalties.penalizedScore(200.0, 8, 44.9) == 200.0)
    assert(RetreatExtraPenalties.penalizedScore(200.0, 30, 10.0) == 200.0)
  }
}
