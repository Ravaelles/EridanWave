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

  test("contact and safety preempt sharply, downgrades wait out the release") {
    assert(MotionVerdict.owner(MotionVerdict.Advance, MotionVerdict.Duel, stableFrames = 0) == MotionVerdict.Duel)
    assert(MotionVerdict.owner(MotionVerdict.Dress, MotionVerdict.Duel, stableFrames = 0) == MotionVerdict.Duel)
    assert(MotionVerdict.owner(MotionVerdict.Duel, MotionVerdict.Withdraw, stableFrames = 0) == MotionVerdict.Withdraw)
    assert(MotionVerdict.owner(MotionVerdict.Duel, MotionVerdict.Duel, stableFrames = 0) == MotionVerdict.Duel)
    assert(MotionVerdict.owner(MotionVerdict.Duel, MotionVerdict.Advance, stableFrames = 0) == MotionVerdict.Duel)
    assert(MotionVerdict.owner(MotionVerdict.Duel, MotionVerdict.Advance, stableFrames = MotionVerdict.releaseFrames - 1) == MotionVerdict.Duel)
    assert(MotionVerdict.owner(MotionVerdict.Duel, MotionVerdict.Advance, stableFrames = MotionVerdict.releaseFrames) == MotionVerdict.Advance)
    assert(MotionVerdict.owner(MotionVerdict.Duel, MotionVerdict.Dress, stableFrames = MotionVerdict.releaseFrames) == MotionVerdict.Dress)
    assert(MotionVerdict.owner(MotionVerdict.Dress, MotionVerdict.Advance, stableFrames = 0) == MotionVerdict.Dress)
  }

  test("an unknown unit holds no verdict") {
    MotionVerdict.reset()
    assert(MotionVerdict.held(id = 7, frame = 100) == MotionVerdict.Advance)
  }

  test("the bus enters sharp on contact") {
    MotionVerdict.reset()
    MotionVerdict.update(id = 7, mode = MotionVerdict.Duel, frame = 100)
    assert(MotionVerdict.held(id = 7, frame = 100) == MotionVerdict.Duel)
  }

  test("a one-frame target flicker never releases the duel") {
    MotionVerdict.reset()
    MotionVerdict.update(id = 7, mode = MotionVerdict.Duel, frame = 100)
    MotionVerdict.update(id = 7, mode = MotionVerdict.Advance, frame = 101)
    MotionVerdict.update(id = 7, mode = MotionVerdict.Duel, frame = 102)
    assert(MotionVerdict.held(id = 7, frame = 102) == MotionVerdict.Duel)
    MotionVerdict.update(id = 7, mode = MotionVerdict.Advance, frame = 103)
    assert(MotionVerdict.held(id = 7, frame = 103) == MotionVerdict.Duel)
  }

  test("a genuinely finished duel hands the march back past the release") {
    MotionVerdict.reset()
    MotionVerdict.update(id = 7, mode = MotionVerdict.Duel, frame = 100)
    MotionVerdict.update(id = 7, mode = MotionVerdict.Advance, frame = 101)
    MotionVerdict.update(id = 7, mode = MotionVerdict.Advance, frame = 102)
    MotionVerdict.update(id = 7, mode = MotionVerdict.Advance, frame = 103)
    assert(MotionVerdict.held(id = 7, frame = 103) == MotionVerdict.Duel)
    MotionVerdict.update(id = 7, mode = MotionVerdict.Advance, frame = 104)
    assert(MotionVerdict.held(id = 7, frame = 104) == MotionVerdict.Advance)
  }

  test("a same-frame fresh duel beats a stale march read, either tick order") {
    MotionVerdict.reset()
    MotionVerdict.update(id = 7, mode = MotionVerdict.Duel, frame = 100)
    MotionVerdict.update(id = 7, mode = MotionVerdict.Advance, frame = 101)
    MotionVerdict.update(id = 7, mode = MotionVerdict.Duel, frame = 101)
    assert(MotionVerdict.held(id = 7, frame = 101) == MotionVerdict.Duel)
    MotionVerdict.reset()
    MotionVerdict.update(id = 7, mode = MotionVerdict.Duel, frame = 100)
    MotionVerdict.update(id = 7, mode = MotionVerdict.Duel, frame = 101)
    MotionVerdict.update(id = 7, mode = MotionVerdict.Advance, frame = 101)
    assert(MotionVerdict.held(id = 7, frame = 101) == MotionVerdict.Duel)
  }

  test("units do not share a verdict") {
    MotionVerdict.reset()
    MotionVerdict.update(id = 7, mode = MotionVerdict.Duel, frame = 100)
    assert(MotionVerdict.held(id = 8, frame = 100) == MotionVerdict.Advance)
  }

  test("reset clears the bus for the next game") {
    MotionVerdict.update(id = 7, mode = MotionVerdict.Duel, frame = 100)
    MotionVerdict.reset()
    assert(MotionVerdict.held(id = 7, frame = 100) == MotionVerdict.Advance)
  }
}
