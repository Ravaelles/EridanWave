package Tactic.Squads.attack

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarSplitSuite extends AnyFunSuite {

  test("zero or one Templar means zero raiders") {
    assert(DarkTemplarSplit.raidersToTake(0) == 0)
    assert(DarkTemplarSplit.raidersToTake(1) == 0)
  }

  test("exactly one Templar stays with the army") {
    assert(DarkTemplarSplit.raidersToTake(2) == 1)
    assert(DarkTemplarSplit.raidersToTake(4) == 3)
  }
}
