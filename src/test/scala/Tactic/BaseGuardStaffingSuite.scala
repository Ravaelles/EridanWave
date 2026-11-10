package Tactic

import org.scalatest.funsuite.AnyFunSuite

class BaseGuardStaffingSuite extends AnyFunSuite {

  test("small armies spare nobody") {
    assert(BaseGuardStaffing.guardsFor(0) == 0)
    assert(BaseGuardStaffing.guardsFor(20) == 0)
  }

  test("one guard from twenty-one, one more per six") {
    assert(BaseGuardStaffing.guardsFor(21) == 1)
    assert(BaseGuardStaffing.guardsFor(25) == 1)
    assert(BaseGuardStaffing.guardsFor(26) == 2)
    assert(BaseGuardStaffing.guardsFor(32) == 3)
    assert(BaseGuardStaffing.guardsFor(38) == 4)
  }
}
