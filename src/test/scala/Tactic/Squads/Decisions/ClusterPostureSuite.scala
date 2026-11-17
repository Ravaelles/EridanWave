package Tactic.Squads.Decisions

import Tactic.Squads.Decisions.ClusterPosture._
import org.scalatest.funsuite.AnyFunSuite

class ClusterPostureSuite extends AnyFunSuite {

  private def next(
    current: Posture = Fight,
    fighting: Double = 1.0,
    coverage: Double = 1.0,
    advantage: Double = 2.0,
    inContact: Boolean = true,
    sinceWithdraw: Int = 1000,
    retreatAllowed: Boolean = true): Posture =
    ClusterPosture.next(current, fighting, coverage, advantage, inContact, sinceWithdraw, retreatAllowed = retreatAllowed)

  test("no forecast never flips anything") {
    assert(next(Fight, coverage = 0.4) == Fight)
    assert(next(Withdraw, coverage = 0.4, sinceWithdraw = 0) == Withdraw)
    assert(next(HoldLine, coverage = 0.0) == HoldLine)
    assert(next(Regroup, coverage = 0.49) == Regroup)
  }

  test("a collapsing sim vote withdraws from any posture") {
    assert(next(Fight, fighting = 0.2) == Withdraw)
    assert(next(HoldLine, fighting = 0.0) == Withdraw)
    assert(next(Regroup, fighting = 0.34, inContact = false) == Withdraw)
  }

  test("the floor holds the withdrawal against any forecast") {
    assert(next(Withdraw, fighting = 1.0, advantage = 10.0, sinceWithdraw = 0) == Withdraw)
    assert(next(Withdraw, fighting = 1.0, advantage = 10.0, sinceWithdraw = 95) == Withdraw)
  }

  test("leaving takes sim agreement plus parity, then reads the band") {
    assert(next(Withdraw, fighting = 0.5, advantage = 5.0, sinceWithdraw = 500) == Withdraw)
    assert(next(Withdraw, fighting = 0.9, advantage = 0.9, sinceWithdraw = 500) == Withdraw)
    assert(next(Withdraw, fighting = 0.9, advantage = 1.2, sinceWithdraw = 500) == HoldLine)
    assert(next(Withdraw, fighting = 0.9, advantage = 1.2, sinceWithdraw = 500, inContact = false) == Regroup)
    assert(next(Withdraw, fighting = 0.9, advantage = 2.0, sinceWithdraw = 500) == Fight)
  }

  test("superiority fights, parity holds the line, calm regroups") {
    assert(next(Fight, advantage = 2.0) == Fight)
    assert(next(Regroup, fighting = 0.8, advantage = 2.0) == Fight)
    assert(next(Fight, fighting = 0.8, advantage = 1.2) == HoldLine)
    assert(next(HoldLine, fighting = 0.8, advantage = 1.2) == HoldLine)
    assert(next(HoldLine, fighting = 0.8, advantage = 1.2, inContact = false) == Regroup)
  }

  test("the Fight edge carries a deadband, never the line itself") {
    assert(next(Fight, fighting = 0.8, advantage = 1.4) == Fight)
    assert(next(Fight, fighting = 0.8, advantage = 1.2) == HoldLine)
    assert(next(HoldLine, fighting = 0.8, advantage = 1.4) == HoldLine)
  }

  test("baseless maps never withdraw: contact closes, calm regroups") {
    assert(ClusterPosture.next(Withdraw, 0.0, 1.0, 0.5, inContact = true, 0, canWithdraw = false) == Fight)
    assert(ClusterPosture.next(Fight, 0.0, 1.0, 0.5, inContact = true, 500, canWithdraw = false) == Fight)
    assert(ClusterPosture.next(Fight, 0.8, 1.0, 2.0, inContact = false, 500, canWithdraw = false) == Regroup)
    assert(ClusterPosture.next(Regroup, 0.0, 1.0, 0.5, inContact = true, 0, canWithdraw = false) == Fight)
  }

  test("only Fight and HoldLine duel") {
    assert(ClusterPosture.advances(Fight))
    assert(ClusterPosture.advances(HoldLine))
    assert( ! ClusterPosture.advances(Withdraw))
    assert( ! ClusterPosture.advances(Regroup))
  }

  test("a forbidden retreat reads the band instead of withdrawing") {
    assert(next(Fight, fighting = 0.2, retreatAllowed = false) == Fight)
    assert(next(HoldLine, fighting = 0.0, advantage = 1.2, retreatAllowed = false) == HoldLine)
    assert(next(Regroup, fighting = 0.0, advantage = 1.2, inContact = false, retreatAllowed = false) == Regroup)
    assert(next(Withdraw, fighting = 0.0, advantage = 1.2, sinceWithdraw = 0, retreatAllowed = false) == HoldLine)
    assert(next(Withdraw, fighting = 1.0, advantage = 10.0, sinceWithdraw = 0, retreatAllowed = false) == Fight)
  }
}
