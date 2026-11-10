package Tactic.Production.construction

import org.scalatest.funsuite.AnyFunSuite

class ExpoBuildShiftSuite extends AnyFunSuite {

  test("seventy-two arrived frames is still patience") {
    assert( ! ExpoBuildShift.stalled(0))
    assert( ! ExpoBuildShift.stalled(72))
  }

  test("past seventy-two arrived frames means blocked") {
    assert(ExpoBuildShift.stalled(73))
    assert(ExpoBuildShift.stalled(5000))
  }
}
