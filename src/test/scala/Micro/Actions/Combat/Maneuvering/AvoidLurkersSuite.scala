package Micro.Actions.Combat.Maneuvering

import org.scalatest.funsuite.AnyFunSuite

class AvoidLurkersSuite extends AnyFunSuite {

  test("scarabs let a Reaver close Lurkers, everything else dodges wide") {
    assert(AvoidLurkers.avoidTilesFor(isReaver = true, scarabs = 1) == 7.05)
    assert(AvoidLurkers.avoidTilesFor(isReaver = true, scarabs = 5) == 7.05)
    assert(AvoidLurkers.avoidTilesFor(isReaver = true, scarabs = 0) == 8.0)
    assert(AvoidLurkers.avoidTilesFor(isReaver = false, scarabs = 5) == 8.0)
    assert(AvoidLurkers.avoidTilesFor(isReaver = false, scarabs = 0) == 8.0)
  }
}
