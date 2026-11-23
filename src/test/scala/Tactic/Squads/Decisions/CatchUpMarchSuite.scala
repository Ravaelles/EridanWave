package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class CatchUpMarchSuite extends AnyFunSuite {

  test("pins the catch-up trail") {
    assert(CatchUpMarch.behindMarginTiles == 5.0)
    assert(CatchUpMarch.behindEnterTiles == 6.0)
    assert(CatchUpMarch.behindExitTiles == 4.0)
  }

  test("a reinforcement five tiles back joins at full speed") {
    assert(CatchUpMarch.shouldCatchUp(behindTiles = 5.1))
    assert(CatchUpMarch.shouldCatchUp(behindTiles = 20.0))
    assert( ! CatchUpMarch.shouldCatchUp(behindTiles = 5.0))
    assert( ! CatchUpMarch.shouldCatchUp(behindTiles = 0.0))
    assert( ! CatchUpMarch.shouldCatchUp(behindTiles = -10.0))
  }

  test("the latch fires past six and releases inside four") {
    CatchUpMarch.reset()
    assert(CatchUpMarch.catchUp(1, behindTiles = 6.1))
    assert(CatchUpMarch.catchUp(1, behindTiles = 5.0))
    assert( ! CatchUpMarch.catchUp(1, behindTiles = 3.9))
    assert( ! CatchUpMarch.catchUp(1, behindTiles = 5.0))
  }

  test("a fresh unit only catches up past the sharp edge") {
    CatchUpMarch.reset()
    assert(CatchUpMarch.catchUp(7, behindTiles = 6.1))
    assert( ! CatchUpMarch.catchUp(8, behindTiles = 5.0))
  }
}
