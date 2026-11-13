package Micro.Actions.Protoss.Arbiter

import org.scalatest.funsuite.AnyFunSuite

class ArbiterFleeSuite extends AnyFunSuite {

  test("fresh hulls hold till two tiles, battered ones break off early") {
    assert(BeArbiter.fleeMarginTiles(0.0) == 2.0 + 4.0)
    assert(BeArbiter.fleeMarginTiles(1.0) == 2.0 + 9.0)
    assert(BeArbiter.fleeMarginTiles(0.5) == 2.0 + 6.5)
  }
}
