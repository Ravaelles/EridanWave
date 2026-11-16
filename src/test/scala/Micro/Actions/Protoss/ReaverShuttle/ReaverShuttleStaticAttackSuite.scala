package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverShuttleStaticAttackSuite extends AnyFunSuite {

  test("inside the floor the hull steps out, outside it holds the line") {
    assert(ReaverShuttleStaticAttack.shouldStepOut(7.05, 7.05))
    assert(ReaverShuttleStaticAttack.shouldStepOut(0.0, 7.85))
    assert( ! ReaverShuttleStaticAttack.shouldStepOut(7.06, 7.05))
    assert( ! ReaverShuttleStaticAttack.shouldStepOut(8.0, 7.85))
  }

  test("the base floor sits just past the longest static gun") {
    assert(ReaverShuttleStaticAttack.keepOffTiles == 7.05)
    assert(ReaverShuttleStaticAttack.stepTiles == 0.5)
  }
}
