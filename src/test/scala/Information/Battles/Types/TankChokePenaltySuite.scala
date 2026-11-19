package Information.Battles.Types

import org.scalatest.funsuite.AnyFunSuite

class TankChokePenaltySuite extends AnyFunSuite {

  private def penalizes(
    tankDistTiles: Double = TankChokePenalty.tankRadiusTiles,
    chokeDistTiles: Double = TankChokePenalty.chokeRadiusTiles - 0.1): Boolean =
    TankChokePenalty.shouldPenalize(tankDistTiles, chokeDistTiles)

  test("guns over the lane hike the bar") {
    assert(penalizes())
  }

  test("tanks past twenty tiles do not cover us") {
    assert( ! penalizes(tankDistTiles = TankChokePenalty.tankRadiusTiles + 0.1))
  }

  test("eight tiles from the choke is past the lane") {
    assert(penalizes(chokeDistTiles = TankChokePenalty.chokeRadiusTiles - 0.1))
    assert( ! penalizes(chokeDistTiles = TankChokePenalty.chokeRadiusTiles))
  }
}
