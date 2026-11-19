package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class RetreatDisciplineSuite extends AnyFunSuite {

  test("a withdrawal lasts two seconds whatever the forecast says") {
    assert(RetreatDiscipline.shouldHoldWithdraw(0))
    assert(RetreatDiscipline.shouldHoldWithdraw(47))
    assert( ! RetreatDiscipline.shouldHoldWithdraw(48))
    assert( ! RetreatDiscipline.shouldHoldWithdraw(1000))
  }

  test("fresh wounds read weakest, old ones read whole") {
    assert(RetreatDiscipline.advantageMultiplier(0) == 0.6)
    assert(RetreatDiscipline.advantageMultiplier(23) == 0.6)
    assert(RetreatDiscipline.advantageMultiplier(24) == 0.7)
    assert(RetreatDiscipline.advantageMultiplier(47) == 0.7)
    assert(RetreatDiscipline.advantageMultiplier(48) == 0.8)
    assert(RetreatDiscipline.advantageMultiplier(71) == 0.8)
    assert(RetreatDiscipline.advantageMultiplier(72) == 0.85)
    assert(RetreatDiscipline.advantageMultiplier(144) == 0.85)
    assert(RetreatDiscipline.advantageMultiplier(145) == 1.0)
    assert(RetreatDiscipline.advantageMultiplier(10000) == 1.0)
  }

  test("the floor matches the two-second minimum") {
    assert(RetreatDiscipline.minWithdrawFrames == 48)
  }
}
