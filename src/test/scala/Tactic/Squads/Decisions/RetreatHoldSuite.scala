package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class RetreatHoldSuite extends AnyFunSuite {

  test("a fresh retreat holds at once") {
    assert(RetreatHold.held(rawHold = true, framesSinceFightEdge = 999, holdFrames = 24))
  }

  test("a steady fight releases the hold") {
    assert( ! RetreatHold.held(rawHold = false, framesSinceFightEdge = 24, holdFrames = 24))
    assert( ! RetreatHold.held(rawHold = false, framesSinceFightEdge = 500, holdFrames = 24))
  }

  test("a flickered fight inside the window cannot release") {
    assert(RetreatHold.held(rawHold = false, framesSinceFightEdge = 0, holdFrames = 24))
    assert(RetreatHold.held(rawHold = false, framesSinceFightEdge = 23, holdFrames = 24))
  }

  test("re-entering retreat re-arms the full window") {
    assert(RetreatHold.held(rawHold = true, framesSinceFightEdge = 0, holdFrames = 24))
    assert(RetreatHold.held(rawHold = false, framesSinceFightEdge = 0, holdFrames = 24))
  }

  test("zero disables the hold") {
    assert( ! RetreatHold.held(rawHold = false, framesSinceFightEdge = 0, holdFrames = 0))
    assert(RetreatHold.held(rawHold = true, framesSinceFightEdge = 0, holdFrames = 0))
  }
}
