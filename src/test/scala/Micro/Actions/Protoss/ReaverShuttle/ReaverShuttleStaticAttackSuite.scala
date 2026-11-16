package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverShuttleStaticAttackSuite extends AnyFunSuite {

  test("inside the floor the hull steps out, outside it holds the line") {
    assert(ReaverShuttleStaticAttack.shouldStepOut(7.3))
    assert(ReaverShuttleStaticAttack.shouldStepOut(0.0))
    assert( ! ReaverShuttleStaticAttack.shouldStepOut(7.31))
    assert( ! ReaverShuttleStaticAttack.shouldStepOut(8.0))
  }

  test("the floor sits past the longest static reach with one step of air") {
    assert(ReaverShuttleStaticAttack.keepOffTiles > 7.0)
    assert(ReaverShuttleStaticAttack.stepTiles == 0.5)
  }
}
