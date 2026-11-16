package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class PreyDriveSuite extends AnyFunSuite {

  private def hunts(
    workersOnly: Boolean = false,
    usSkim: Double = 0.0,
    foeSkim: Double = 0.0): Boolean =
    PreyDrive.shouldHunt(workersOnly, usSkim, foeSkim)

  test("workers only means hunt, whatever the skim") {
    assert(hunts(workersOnly = true))
    assert(hunts(workersOnly = true, usSkim = 10.0, foeSkim = 100.0))
  }

  test("three-to-one local skim hunts") {
    assert(hunts(usSkim = 300.0, foeSkim = 100.0))
    assert(hunts(usSkim = 3.0, foeSkim = 1.0))
  }

  test("even fights and empty fields keep the formation") {
    assert( ! hunts(usSkim = 200.0, foeSkim = 100.0))
    assert( ! hunts())
    assert( ! hunts(usSkim = 100.0, foeSkim = 0.0))
  }
}
