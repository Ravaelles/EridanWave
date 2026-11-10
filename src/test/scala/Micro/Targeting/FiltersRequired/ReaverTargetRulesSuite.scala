package Micro.Targeting.FiltersRequired

import org.scalatest.funsuite.AnyFunSuite

class ReaverTargetRulesSuite extends AnyFunSuite {

  test("units are always allowed") {
    assert(ReaverTargetRules.allowsBuilding(isBuilding = false, isStaticDefense = false))
  }

  test("static defense is allowed, Bunkers included") {
    assert(ReaverTargetRules.allowsBuilding(isBuilding = true, isStaticDefense = true))
  }

  test("other buildings are the army's job") {
    assert( ! ReaverTargetRules.allowsBuilding(isBuilding = true, isStaticDefense = false))
  }
}
