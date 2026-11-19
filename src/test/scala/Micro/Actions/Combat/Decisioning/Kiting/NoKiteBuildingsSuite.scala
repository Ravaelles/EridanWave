package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

class NoKiteBuildingsSuite extends AnyFunSuite {

  test("a building anchor holds instead of yielding") {
    assert(KiteAway.holdAgainstBuildings(anchorIsBuilding = true))
    assert( ! KiteAway.holdAgainstBuildings(anchorIsBuilding = false))
  }
}
