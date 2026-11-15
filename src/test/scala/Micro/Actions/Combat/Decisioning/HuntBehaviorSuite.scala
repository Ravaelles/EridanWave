package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class HuntBehaviorSuite extends AnyFunSuite {

  test("a dueling walker with no target hunts") {
    assert(HuntBehavior.shouldHunt(
      umsMode   = true,
      isWarrior = true,
      isReaver  = false,
      canMove   = true,
      hasTarget = false))
  }

  test("macro columns never chase remembered contacts") {
    assert( ! HuntBehavior.shouldHunt(
      umsMode   = false,
      isWarrior = true,
      isReaver  = false,
      canMove   = true,
      hasTarget = false))
  }

  test("reavers keep the pair doctrine") {
    assert( ! HuntBehavior.shouldHunt(
      umsMode   = true,
      isWarrior = true,
      isReaver  = true,
      canMove   = true,
      hasTarget = false))
  }

  test("a unit with a target fights it instead of hunting") {
    assert( ! HuntBehavior.shouldHunt(
      umsMode   = true,
      isWarrior = true,
      isReaver  = false,
      canMove   = true,
      hasTarget = true))
  }

  test("immobile hulls never hunt") {
    assert( ! HuntBehavior.shouldHunt(
      umsMode   = true,
      isWarrior = true,
      isReaver  = false,
      canMove   = false,
      hasTarget = false))
  }

  test("non-warriors never hunt") {
    assert( ! HuntBehavior.shouldHunt(
      umsMode   = true,
      isWarrior = false,
      isReaver  = false,
      canMove   = true,
      hasTarget = false))
  }
}
