package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class WithdrawAnchorSuite extends AnyFunSuite {

  test("a reached middle choke anchors the withdrawal") {
    assert(WithdrawAnchor.anchor(0.0, 0.0, Some((100.0, 200.0)), reachesChoke = true) == ((100.0, 200.0)))
  }

  test("no choke, or no path, falls back home") {
    assert(WithdrawAnchor.anchor(0.0, 0.0, None, reachesChoke = true) == ((0.0, 0.0)))
    assert(WithdrawAnchor.anchor(0.0, 0.0, Some((100.0, 200.0)), reachesChoke = false) == ((0.0, 0.0)))
  }
}
