package Micro.Actions.Combat.Decisioning.Skirmish

import org.scalatest.funsuite.AnyFunSuite

class NoRetreatSuite extends AnyFunSuite {

  test("siege tanks inside eighteen tiles of the natural must die standing") {
    assert(NoRetreat.threatensNatural(18.0))
    assert(NoRetreat.threatensNatural(0.0))
    assert( ! NoRetreat.threatensNatural(18.01))
  }
}
