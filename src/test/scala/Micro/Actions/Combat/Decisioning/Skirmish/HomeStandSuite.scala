package Micro.Actions.Combat.Decisioning.Skirmish

import org.scalatest.funsuite.AnyFunSuite

class HomeStandSuite extends AnyFunSuite {

  test("between main and the choke margin the unit holds") {
    assert(HomeStand.holdsGround(unitDistTiles = 0.0, chokeDistTiles = 20.0))
    assert(HomeStand.holdsGround(unitDistTiles = 16.0, chokeDistTiles = 20.0))
    assert( ! HomeStand.holdsGround(unitDistTiles = 16.01, chokeDistTiles = 20.0))
    assert( ! HomeStand.holdsGround(unitDistTiles = 25.0, chokeDistTiles = 20.0))
  }
}
