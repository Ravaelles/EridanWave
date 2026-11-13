package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverTanksSuite extends AnyFunSuite {

  test("a sieged tank closer than ten tiles shoots the landed Reaver") {
    assert(ReaverTanks.aiming(0.0))
    assert(ReaverTanks.aiming(9.99))
    assert( ! ReaverTanks.aiming(10.0))
  }

  test("a sieged tank covers its range plus the margin") {
    assert(ReaverTanks.covering(tankRangeTiles = 12.0, distTiles = 0.0))
    assert(ReaverTanks.covering(tankRangeTiles = 12.0, distTiles = 13.0))
    assert( ! ReaverTanks.covering(tankRangeTiles = 12.0, distTiles = 13.01))
  }

  test("only tanks inside the theater vote on the drop") {
    assert(ReaverTanks.inTheater(0.0))
    assert(ReaverTanks.inTheater(19.99))
    assert( ! ReaverTanks.inTheater(20.0))
    assert(ReaverTanks.theaterTiles == 20.0)
  }
}
