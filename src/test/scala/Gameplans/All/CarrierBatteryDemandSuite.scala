package Gameplans.All

import org.scalatest.funsuite.AnyFunSuite

class CarrierBatteryDemandSuite extends AnyFunSuite {

  test("batteries scale with the wing: none, one, two") {
    assert(CarrierBatteryDemand.wantBatteries(0) == 0)
    assert(CarrierBatteryDemand.wantBatteries(7) == 0)
    assert(CarrierBatteryDemand.wantBatteries(8) == 1)
    assert(CarrierBatteryDemand.wantBatteries(19) == 1)
    assert(CarrierBatteryDemand.wantBatteries(20) == 2)
    assert(CarrierBatteryDemand.wantBatteries(60) == 2)
  }
}
