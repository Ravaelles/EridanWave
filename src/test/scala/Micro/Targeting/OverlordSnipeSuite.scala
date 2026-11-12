package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class OverlordSnipeSuite extends AnyFunSuite {

  test("a lone Overlord in range is sniped") {
    assert(OverlordSnipe.allowed(overlordInRange = true, alternativesInRange = 0))
  }

  test("an out-of-range Overlord is never chased") {
    assert( ! OverlordSnipe.allowed(overlordInRange = false, alternativesInRange = 0))
    assert( ! OverlordSnipe.allowed(overlordInRange = false, alternativesInRange = 3))
  }

  test("anything else in range outranks the Overlord, workers included") {
    assert( ! OverlordSnipe.allowed(overlordInRange = true, alternativesInRange = 1))
    assert( ! OverlordSnipe.allowed(overlordInRange = true, alternativesInRange = 5))
  }
}
