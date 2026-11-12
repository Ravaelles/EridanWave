package Placement.Pylons

import org.scalatest.funsuite.AnyFunSuite

class SecondPylonPlacementSuite extends AnyFunSuite {

  test("the second pylon stays near the town hall") {
    assert(SecondPylonPlacement.nearTownHall(0))
    assert(SecondPylonPlacement.nearTownHall(4))
    assert(SecondPylonPlacement.nearTownHall(8))
  }

  test("distant supply fits are rejected") {
    assert( ! SecondPylonPlacement.nearTownHall(9))
    assert( ! SecondPylonPlacement.nearTownHall(25))
  }
}
