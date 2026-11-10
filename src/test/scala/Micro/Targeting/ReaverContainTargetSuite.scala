package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class ReaverContainTargetSuite extends AnyFunSuite {

  test("a warrior-less Reaver bonuses defensive buildings") {
    assert(ReaverContainTarget.bonus(isReaver = true, warriorInRange = false, isStaticDefense = true) == 12.0)
  }

  test("warriors in range need no bonus") {
    assert(ReaverContainTarget.bonus(isReaver = true, warriorInRange = true, isStaticDefense = true) == 0.0)
  }

  test("non-Reavers and non-buildings get nothing") {
    assert(ReaverContainTarget.bonus(isReaver = false, warriorInRange = false, isStaticDefense = true) == 0.0)
    assert(ReaverContainTarget.bonus(isReaver = true, warriorInRange = false, isStaticDefense = false) == 0.0)
    assert(ReaverContainTarget.bonus(isReaver = false, warriorInRange = false, isStaticDefense = false) == 0.0)
  }
}
