package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class RetreatAllowedSuite extends AnyFunSuite {

  test("weak clusters retreat from near enemies, never from empty fields") {
    assert(RetreatAllowed.forCluster(unitsScore = 100.0, nearestArmedTiles = 3.0, proxyRush = false, hardCounterKnown = false))
    assert(RetreatAllowed.forCluster(unitsScore = 179.9, nearestArmedTiles = 11.9, proxyRush = false, hardCounterKnown = false))
    assert( ! RetreatAllowed.forCluster(unitsScore = 100.0, nearestArmedTiles = 40.0, proxyRush = false, hardCounterKnown = false))
    assert( ! RetreatAllowed.forCluster(unitsScore = 100.0, nearestArmedTiles = Double.PositiveInfinity, proxyRush = false, hardCounterKnown = false))
  }

  test("a strong cluster on a clear field never retreats") {
    assert( ! RetreatAllowed.forCluster(unitsScore = 250.0, nearestArmedTiles = 40.0, proxyRush = false, hardCounterKnown = false))
    assert( ! RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = Double.PositiveInfinity, proxyRush = false, hardCounterKnown = false))
    assert(RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = 3.0, proxyRush = false, hardCounterKnown = true))
  }

  test("at or above 180 the cluster holds without asking the sim") {
    assert( ! RetreatAllowed.forCluster(unitsScore = 180.0, nearestArmedTiles = 40.0, proxyRush = false, hardCounterKnown = false))
    assert( ! RetreatAllowed.forCluster(unitsScore = 180.0, nearestArmedTiles = 3.0, proxyRush = false, hardCounterKnown = false))
    assert( ! RetreatAllowed.forCluster(unitsScore = 500.0, nearestArmedTiles = 3.0, proxyRush = false, hardCounterKnown = false))
  }

  test("a known hard counter restores the old vote") {
    assert(RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = 12.0, proxyRush = false, hardCounterKnown = true))
    assert(RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = 3.0, proxyRush = false, hardCounterKnown = true))
    assert( ! RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = 12.01, proxyRush = false, hardCounterKnown = true))
    assert( ! RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = false, hardCounterKnown = true))
    assert( ! RetreatAllowed.forCluster(unitsScore = 500.0, nearestArmedTiles = 3.0, proxyRush = true, hardCounterKnown = true))
  }

  test("a proxy rush forbids retreat outright") {
    assert( ! RetreatAllowed.forCluster(unitsScore = 100.0, nearestArmedTiles = 3.0, proxyRush = true, hardCounterKnown = false))
    assert( ! RetreatAllowed.forCluster(unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = true, hardCounterKnown = false))
  }

  test("a dying hull always keeps its own way out") {
    assert(RetreatAllowed.forUnit(totalHp = 35, unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = true, hardCounterKnown = false))
    assert(RetreatAllowed.forUnit(totalHp = 10, unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = false, hardCounterKnown = false))
    assert( ! RetreatAllowed.forUnit(totalHp = 36, unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = true, hardCounterKnown = false))
    assert(RetreatAllowed.forUnit(totalHp = 200, unitsScore = 100.0, nearestArmedTiles = 3.0, proxyRush = false, hardCounterKnown = false))
    assert( ! RetreatAllowed.forUnit(totalHp = 200, unitsScore = 100.0, nearestArmedTiles = 40.0, proxyRush = false, hardCounterKnown = false))
  }
}
