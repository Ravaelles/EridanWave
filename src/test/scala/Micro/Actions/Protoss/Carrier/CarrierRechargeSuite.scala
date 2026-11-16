package Micro.Actions.Protoss.Carrier

import org.scalatest.funsuite.AnyFunSuite

class CarrierRechargeSuite extends AnyFunSuite {

  test("a beat hull with an empty shield goes home") {
    assert(CarrierRecharge.shouldRetreat(health01 = 0.4, shields = 0))
    assert(CarrierRecharge.shouldRetreat(health01 = 0.49, shields = 19))
  }

  test("healthy hulls and charged shields stay in the fight") {
    assert( ! CarrierRecharge.shouldRetreat(health01 = 0.5, shields = 0))
    assert( ! CarrierRecharge.shouldRetreat(health01 = 0.4, shields = 20))
    assert( ! CarrierRecharge.shouldRetreat(health01 = 1.0, shields = 150))
  }
}
