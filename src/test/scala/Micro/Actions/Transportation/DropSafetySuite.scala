package Micro.Actions.Transportation

import org.scalatest.funsuite.AnyFunSuite

class DropSafetySuite extends AnyFunSuite {

  test("a Shuttle never unloads inside static range") {
    assert(DropSafety.vetoLanding(transportFlying = true, Some(0.0)))
    assert(DropSafety.vetoLanding(transportFlying = true, Some(8.0)))
    assert( ! DropSafety.vetoLanding(transportFlying = true, Some(8.1)))
    assert( ! DropSafety.vetoLanding(transportFlying = true, Some(20.0)))
    assert( ! DropSafety.vetoLanding(transportFlying = true, None))
  }

  test("grounded unloads ignore the berth") {
    assert( ! DropSafety.vetoLanding(transportFlying = false, Some(0.0)))
    assert( ! DropSafety.vetoLanding(transportFlying = false, None))
  }
}
