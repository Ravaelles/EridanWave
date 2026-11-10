package Placement.Pylons

import PylonPlacement.{FirstPylon, SecondPylon, SupplyPylon}
import org.scalatest.funsuite.AnyFunSuite

class PylonPlacementSuite extends AnyFunSuite {

  test("no Pylons means the Stardust-style edge Pylon") {
    assert(PylonPlacement.caseFor(0) == FirstPylon)
    assert(PylonPlacement.caseFor(-1) == FirstPylon)
  }

  test("one Pylon means the main-base tech Pylon") {
    assert(PylonPlacement.caseFor(1) == SecondPylon)
  }

  test("later Pylons are generic supply") {
    assert(PylonPlacement.caseFor(2) == SupplyPylon)
    assert(PylonPlacement.caseFor(9) == SupplyPylon)
  }
}
