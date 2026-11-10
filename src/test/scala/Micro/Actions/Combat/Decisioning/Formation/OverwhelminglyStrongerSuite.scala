package Micro.Actions.Combat.Decisioning.Formation

import org.scalatest.funsuite.AnyFunSuite

class OverwhelminglyStrongerSuite extends AnyFunSuite {

  test("four times stronger skips formation") {
    assert(OverwhelminglyStronger.locally(400.0, 100.0))
    assert(OverwhelminglyStronger.locally(800.0, 100.0))
  }

  test("below four times dresses normally") {
    assert( ! OverwhelminglyStronger.locally(399.9, 100.0))
    assert( ! OverwhelminglyStronger.locally(100.0, 100.0))
    assert( ! OverwhelminglyStronger.locally(0.0, 100.0))
  }

  test("no known enemy strength means nothing to dress against") {
    assert(OverwhelminglyStronger.locally(10.0, 0.0))
    assert(OverwhelminglyStronger.locally(0.0, 0.0))
  }
}
