package Tactic.Squads.attack

import org.scalatest.funsuite.AnyFunSuite

class SquadRazeExpansionSuite extends AnyFunSuite {

  test("a full detail takes nothing") {
    assert(SquadRazeExpansion.donorsNeeded(mine = 10, attackWarriors = 30) == 0)
    assert(SquadRazeExpansion.donorsNeeded(mine = 12, attackWarriors = 30) == 0)
  }

  test("an empty detail takes ten from a deep army") {
    assert(SquadRazeExpansion.donorsNeeded(mine = 0, attackWarriors = 30) == 10)
    assert(SquadRazeExpansion.donorsNeeded(mine = 4, attackWarriors = 30) == 6)
  }

  test("the attack reserve never donates") {
    assert(SquadRazeExpansion.donorsNeeded(mine = 0, attackWarriors = 10) == 0)
    assert(SquadRazeExpansion.donorsNeeded(mine = 0, attackWarriors = 15) == 5)
    assert(SquadRazeExpansion.donorsNeeded(mine = 8, attackWarriors = 11) == 1)
  }

  test("a weak army raids four while keeping six") {
    assert(SquadRazeExpansion.donorsNeededWeak(mine = 0, attackWarriors = 30) == 4)
    assert(SquadRazeExpansion.donorsNeededWeak(mine = 1, attackWarriors = 30) == 3)
  }

  test("the raid reserve never donates") {
    assert(SquadRazeExpansion.donorsNeededWeak(mine = 0, attackWarriors = 6) == 0)
    assert(SquadRazeExpansion.donorsNeededWeak(mine = 0, attackWarriors = 8) == 2)
  }

  test("a lone raider stays home, a top-up marches") {
    assert(SquadRazeExpansion.donorsNeededWeak(mine = 0, attackWarriors = 7) == 0)
    assert(SquadRazeExpansion.donorsNeededWeak(mine = 1, attackWarriors = 7) == 1)
    assert(SquadRazeExpansion.donorsNeededWeak(mine = 3, attackWarriors = 10) == 1)
  }
}
