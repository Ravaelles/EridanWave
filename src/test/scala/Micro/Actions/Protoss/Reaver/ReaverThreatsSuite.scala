package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverThreatsSuite extends AnyFunSuite {

  test("the engage radius is in tiles") {
    assert(ReaverThreats.inReach(8.5))
    assert(ReaverThreats.inReach(0.0))
    assert( ! ReaverThreats.inReach(8.51))
  }

  test("a sieged tank covers its range plus the margin") {
    assert(ReaverThreats.tankCovered(tankRangeTiles = 12.0, distTiles = 0.0))
    assert(ReaverThreats.tankCovered(tankRangeTiles = 12.0, distTiles = 13.0))
    assert( ! ReaverThreats.tankCovered(tankRangeTiles = 12.0, distTiles = 13.01))
  }
}
