package Micro.Actions.Transportation

import org.scalatest.funsuite.AnyFunSuite

class DropSafetySuite extends AnyFunSuite {

  test("a Shuttle never unloads inside static range") {
    assert(DropSafety.vetoLanding(Some(0.0)))
    assert(DropSafety.vetoLanding(Some(8.0)))
    assert( ! DropSafety.vetoLanding(Some(8.1)))
    assert( ! DropSafety.vetoLanding(Some(20.0)))
    assert( ! DropSafety.vetoLanding(None))
  }
}
