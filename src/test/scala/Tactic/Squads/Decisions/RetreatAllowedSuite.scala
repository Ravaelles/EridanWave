package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class RetreatAllowedSuite extends AnyFunSuite {

  test("a weak cluster may always retreat") {
    assert(RetreatAllowed.forCluster(unitsScore = 100.0, nearestArmedTiles = 40.0, proxyRush = false))
    assert(RetreatAllowed.forCluster(unitsScore = 249.9, nearestArmedTiles = 40.0, proxyRush = false))
    assert(RetreatAllowed.forCluster(unitsScore = 100.0, nearestArmedTiles = 3.0, proxyRush = false))
  }

  test("a strong cluster on a clear field never retreats") {
    assert( ! RetreatAllowed.forCluster(unitsScore = 250.0, nearestArmedTiles = 40.0, proxyRush = false))
    assert( ! RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = Double.PositiveInfinity, proxyRush = false))
  }

  test("a strong cluster with guns nearby may retreat") {
    assert(RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = 12.0, proxyRush = false))
    assert(RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = 3.0, proxyRush = false))
    assert( ! RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = 12.01, proxyRush = false))
  }

  test("a proxy rush forbids retreat outright") {
    assert( ! RetreatAllowed.forCluster(unitsScore = 100.0, nearestArmedTiles = 3.0, proxyRush = true))
    assert( ! RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = true))
  }

  test("a dying hull always keeps its own way out") {
    assert(RetreatAllowed.forUnit(totalHp = 35, unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = true))
    assert(RetreatAllowed.forUnit(totalHp = 10, unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = false))
    assert( ! RetreatAllowed.forUnit(totalHp = 36, unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = true))
    assert(RetreatAllowed.forUnit(totalHp = 200, unitsScore = 100.0, nearestArmedTiles = 40.0, proxyRush = false))
  }
}
