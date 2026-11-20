package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class MotionVerdictSuite extends AnyFunSuite {

  test("a held retreat outranks everything") {
    assert(MotionVerdict.desired(verdictRetreat = true, hasDuel = true, dressing = true) == MotionVerdict.Withdraw)
    assert(MotionVerdict.desired(verdictRetreat = true, hasDuel = false, dressing = false) == MotionVerdict.Withdraw)
  }

  test("contact outranks dressing and the open march") {
    assert(MotionVerdict.desired(verdictRetreat = false, hasDuel = true, dressing = true) == MotionVerdict.Duel)
    assert(MotionVerdict.desired(verdictRetreat = false, hasDuel = true, dressing = false) == MotionVerdict.Duel)
  }

  test("dressing outranks the open march") {
    assert(MotionVerdict.desired(verdictRetreat = false, hasDuel = false, dressing = true) == MotionVerdict.Dress)
  }

  test("nothing claimed means advance") {
    assert(MotionVerdict.desired(verdictRetreat = false, hasDuel = false, dressing = false) == MotionVerdict.Advance)
  }
}
