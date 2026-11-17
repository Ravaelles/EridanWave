package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class MarchRetreatSuite extends AnyFunSuite {

  test("a warrior that will not fight marches home, no questions asked") {
    assert(MarchRetreat.shouldMarchHome(isWarrior = true, isReaver = false, shouldFight = false))
  }

  test("fighters, non-warriors and Reavers keep their legs") {
    assert( ! MarchRetreat.shouldMarchHome(isWarrior = true, isReaver = false, shouldFight = true))
    assert( ! MarchRetreat.shouldMarchHome(isWarrior = false, isReaver = false, shouldFight = false))
    assert( ! MarchRetreat.shouldMarchHome(isWarrior = true, isReaver = true, shouldFight = false))
  }
}
