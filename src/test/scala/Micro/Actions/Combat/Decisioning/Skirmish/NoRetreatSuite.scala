package Micro.Actions.Combat.Decisioning.Skirmish

import org.scalatest.funsuite.AnyFunSuite

class NoRetreatSuite extends AnyFunSuite {

  test("siege tanks inside eighteen tiles of the natural must die standing") {
    assert(NoRetreat.threatensNatural(18.0))
    assert(NoRetreat.threatensNatural(0.0))
    assert( ! NoRetreat.threatensNatural(18.01))
  }

  test("ten seconds in, UMS stops retreating; ladder never does") {
    assert(NoRetreat.umsNoRetreatFrames == 240)
    assert(NoRetreat.umsEndgame(umsMode = true, frame = 240))
    assert(NoRetreat.umsEndgame(umsMode = true, frame = 10000))
    assert( ! NoRetreat.umsEndgame(umsMode = true, frame = 239))
    assert( ! NoRetreat.umsEndgame(umsMode = false, frame = 10000))
  }
}
