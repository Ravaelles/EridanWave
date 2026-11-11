package Tactic.Squads.attack

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarSplitSuite extends AnyFunSuite {

  test("zero Templars means zero raiders") {
    assert(DarkTemplarSplit.raidersToTake(0, 0) == 0)
  }

  test("the first Templar always raids") {
    assert(DarkTemplarSplit.raidersToTake(1, 0) == 1)
    assert(DarkTemplarSplit.raidersToTake(1, 30) == 1)
  }

  test("small armies raid with every blade") {
    assert(DarkTemplarSplit.raidersToTake(2, 10) == 2)
    assert(DarkTemplarSplit.raidersToTake(4, 19) == 4)
  }

  test("grown armies keep exactly one escort") {
    assert(DarkTemplarSplit.raidersToTake(2, 20) == 1)
    assert(DarkTemplarSplit.raidersToTake(4, 60) == 3)
  }
}
