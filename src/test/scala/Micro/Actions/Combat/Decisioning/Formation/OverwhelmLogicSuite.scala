package Micro.Actions.Combat.Decisioning.Formation

import org.scalatest.funsuite.AnyFunSuite

class OverwhelmLogicSuite extends AnyFunSuite {

  test("four times stronger skips formation") {
    assert(OverwhelmLogic.overwhelm(400.0, 100.0))
    assert(OverwhelmLogic.overwhelm(800.0, 100.0))
  }

  test("below four times dresses normally") {
    assert( ! OverwhelmLogic.overwhelm(399.9, 100.0))
    assert( ! OverwhelmLogic.overwhelm(100.0, 100.0))
    assert( ! OverwhelmLogic.overwhelm(0.0, 100.0))
  }

  test("no known enemy strength means nothing to dress against") {
    assert(OverwhelmLogic.overwhelm(10.0, 0.0))
    assert(OverwhelmLogic.overwhelm(0.0, 0.0))
  }
}
