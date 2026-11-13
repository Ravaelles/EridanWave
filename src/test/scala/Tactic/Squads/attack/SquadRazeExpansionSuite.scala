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
}
