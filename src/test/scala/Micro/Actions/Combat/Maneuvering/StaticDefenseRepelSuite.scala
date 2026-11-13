package Micro.Actions.Combat.Maneuvering

import org.scalatest.funsuite.AnyFunSuite

class StaticDefenseRepelSuite extends AnyFunSuite {

  test("outside nine tiles the static does not own the ground") {
    assert( ! StaticDefenseRepel.shouldRepel(9.0))
    assert( ! StaticDefenseRepel.shouldRepel(25.0))
  }

  test("inside nine tiles the unit steps off") {
    assert(StaticDefenseRepel.shouldRepel(8.99))
    assert(StaticDefenseRepel.shouldRepel(0.0))
  }

  test("twelve ours against one static defender means push, not repel") {
    assert(StaticDefenseRepel.locallyAhead(oursNear = 12, foesNearStatic = 1))
    assert(StaticDefenseRepel.locallyAhead(oursNear = 40, foesNearStatic = 0))
    assert( ! StaticDefenseRepel.locallyAhead(oursNear = 11, foesNearStatic = 1))
    assert( ! StaticDefenseRepel.locallyAhead(oursNear = 12, foesNearStatic = 2))
  }
}
