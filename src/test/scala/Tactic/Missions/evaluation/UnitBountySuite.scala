package Tactic.Missions.evaluation

import org.scalatest.funsuite.AnyFunSuite
import ProxyBwapi.Races.{Protoss, Zerg}

class UnitBountySuite extends AnyFunSuite {

  test("standard prices add up") {
    assert(UnitBounty.total(100, 0) == 100)
    assert(UnitBounty.total(125, 50) == 175)
    assert(UnitBounty.total(100, 100) == 200)
  }

  test("pair and egg prices halve, floored") {
    assert(UnitBounty.split(50) == 25)
    assert(UnitBounty.split(25) == 12)
    assert(UnitBounty.split(75) == 37)
    assert(UnitBounty.split(0) == 0)
  }

  test("weighted class bounties weight gas at 1.5") {
    assert(UnitBounty.weightedOf(Protoss.Dragoon) == 200.0)
    assert(UnitBounty.weightedOf(Zerg.Zergling) == 25.0)
    assert(UnitBounty.weightedOf(Zerg.Scourge) == 12.0 + 1.5 * 37.0)
  }

  test("weighted bounty counts gas at 1.5") {
    assert(UnitBounty.weighted(100, 0) == 100.0)
    assert(UnitBounty.weighted(125, 50) == 200.0)
    assert(UnitBounty.weighted(0, 100) == 150.0)
  }
}
