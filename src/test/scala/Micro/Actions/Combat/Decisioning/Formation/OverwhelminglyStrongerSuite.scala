package Micro.Actions.Combat.Decisioning.Formation

import org.scalatest.funsuite.AnyFunSuite

class OverwhelminglyStrongerSuite extends AnyFunSuite {

  test("two-and-a-half times stronger skips formation locally") {
    assert(OverwhelminglyStronger.locally(250.0, 100.0))
    assert(OverwhelminglyStronger.locally(800.0, 100.0))
  }

  test("below two-and-a-half times dresses normally") {
    assert( ! OverwhelminglyStronger.locally(249.9, 100.0))
    assert( ! OverwhelminglyStronger.locally(100.0, 100.0))
    assert( ! OverwhelminglyStronger.locally(0.0, 100.0))
  }

  test("no known enemy strength means nothing to dress against") {
    assert(OverwhelminglyStronger.locally(10.0, 0.0))
    assert(OverwhelminglyStronger.locally(0.0, 0.0))
  }

  test("an unmeasured forecast enemy never skips the formation") {
    assert( ! OverwhelminglyStronger.measuredLocally(1000.0, 0.0))
    assert( ! OverwhelminglyStronger.measuredLocally(0.0, 0.0))
  }

  test("a measured two-and-a-half-to-one still skips locally") {
    assert(OverwhelminglyStronger.measuredLocally(250.0, 100.0))
    assert( ! OverwhelminglyStronger.measuredLocally(249.9, 100.0))
  }

  test("globally overwhelming means four-to-one with a known enemy") {
    assert(OverwhelminglyStronger.globally(400.0, 100.0))
    assert(OverwhelminglyStronger.globally(800.0, 100.0))
    assert( ! OverwhelminglyStronger.globally(399.9, 100.0))
    assert( ! OverwhelminglyStronger.globally(100.0, 100.0))
    assert( ! OverwhelminglyStronger.globally(10.0, 0.0))
    assert( ! OverwhelminglyStronger.globally(0.0, 0.0))
  }
}
