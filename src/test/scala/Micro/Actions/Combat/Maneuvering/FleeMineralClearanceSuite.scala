package Micro.Actions.Combat.Maneuvering

import org.scalatest.funsuite.AnyFunSuite

class FleeMineralClearanceSuite extends AnyFunSuite {

  test("only large ground units keep clear") {
    assert(FleeMineralClearance.applies(isLarge = true, flying = false))
    assert( ! FleeMineralClearance.applies(isLarge = false, flying = false))
    assert( ! FleeMineralClearance.applies(isLarge = true, flying = true))
  }

  test("adjacent minerals veto the spot") {
    assert(FleeMineralClearance.clearOfMinerals(mineralAdjacent = false))
    assert( ! FleeMineralClearance.clearOfMinerals(mineralAdjacent = true))
  }
}
