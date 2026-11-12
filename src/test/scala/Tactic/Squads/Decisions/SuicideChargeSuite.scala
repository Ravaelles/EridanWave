package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class SuicideChargeSuite extends AnyFunSuite {

  test("the charge holds inside thirty land tiles") {
    assert(SuicideCharge.charging(0.0))
    assert(SuicideCharge.charging(30.0))
    assert( ! SuicideCharge.charging(30.01))
    assert( ! SuicideCharge.charging(200.0))
  }
}
