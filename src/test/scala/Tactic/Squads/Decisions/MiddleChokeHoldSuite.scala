package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class MiddleChokeHoldSuite extends AnyFunSuite {

  test("a mid-size army guards the middle choke") {
    assert(MiddleChokeHold.shouldHold(warriors = 14))
    assert(MiddleChokeHold.shouldHold(warriors = 200))
  }

  test("a small force keeps the natural") {
    assert( ! MiddleChokeHold.shouldHold(warriors = 13))
    assert( ! MiddleChokeHold.shouldHold(warriors = 5))
    assert( ! MiddleChokeHold.shouldHold(warriors = 0))
  }
}
