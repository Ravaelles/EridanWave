package Tactic.Squads.attack

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarSplitSuite extends AnyFunSuite {

  test("zero Templars means zero raiders") {
    assert(DarkTemplarSplit.raidersToTake(0) == 0)
  }

  test("the first Templar always raids") {
    assert(DarkTemplarSplit.raidersToTake(1) == 1)
  }

  test("past one, exactly one Templar stays with the army") {
    assert(DarkTemplarSplit.raidersToTake(2) == 1)
    assert(DarkTemplarSplit.raidersToTake(4) == 3)
  }
}
