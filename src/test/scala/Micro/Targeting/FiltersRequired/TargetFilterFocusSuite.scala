package Micro.Targeting.FiltersRequired

import org.scalatest.funsuite.AnyFunSuite

class TargetFilterFocusSuite extends AnyFunSuite {

  test("units and static defense always get the in-range vote") {
    assert(TargetFilterFocus.allowsBuilding(isBuilding = false, isStaticDefense = false))
    assert(TargetFilterFocus.allowsBuilding(isBuilding = true, isStaticDefense = true))
  }

  test("unassigned non-static buildings get no vote") {
    assert( ! TargetFilterFocus.allowsBuilding(isBuilding = true, isStaticDefense = false))
  }
}
