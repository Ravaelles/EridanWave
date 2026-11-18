package Micro.Targeting.FiltersRequired

import org.scalatest.funsuite.AnyFunSuite

class ReaverTargetRulesSuite extends AnyFunSuite {

  test("units are always allowed") {
    assert(ReaverTargetRules.allowsBuilding(isBuilding = false, isCombatBuilding = false))
  }

  test("combat buildings are allowed, Bunkers included") {
    assert(ReaverTargetRules.allowsBuilding(isBuilding = true, isCombatBuilding = true))
  }

  test("Batteries and idle colonies are the army's job") {
    assert( ! ReaverTargetRules.allowsBuilding(isBuilding = true, isCombatBuilding = false))
  }

  test("units never yield to the escort") {
    assert(ReaverTargetRules.yieldsToEscort(isBuilding = false, mobileThreatInRange = true))
    assert(ReaverTargetRules.yieldsToEscort(isBuilding = false, mobileThreatInRange = false))
  }

  test("statics yield while a hittable pack closes") {
    assert( ! ReaverTargetRules.yieldsToEscort(isBuilding = true, mobileThreatInRange = true))
  }

  test("statics stand while the escort is clear") {
    assert(ReaverTargetRules.yieldsToEscort(isBuilding = true, mobileThreatInRange = false))
  }
}
