package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class HuntBehaviorSuite extends AnyFunSuite {

  private def hunt(
    umsMode             : Boolean = true,
    isWarrior           : Boolean = true,
    isReaver            : Boolean = false,
    canMove             : Boolean = true,
    hasTarget           : Boolean = false,
    targetVisible       : Boolean = true,
    targetBeyondPursuit : Boolean = false,
    targetBeyondSight   : Boolean = false): Boolean =
    HuntBehavior.shouldHunt(
      umsMode,
      isWarrior,
      isReaver,
      canMove,
      hasTarget,
      targetVisible,
      targetBeyondPursuit,
      targetBeyondSight)

  test("a dueling walker with no target hunts") {
    assert(hunt())
  }

  test("macro columns never chase remembered contacts") {
    assert( ! hunt(umsMode = false))
  }

  test("reavers keep the pair doctrine") {
    assert( ! hunt(isReaver = true))
  }

  test("a unit with a useful target fights it instead of hunting") {
    assert( ! hunt(hasTarget = true))
  }

  test("immobile hulls never hunt") {
    assert( ! hunt(canMove = false))
  }

  test("non-warriors never hunt") {
    assert( ! hunt(isWarrior = false))
  }

  test("a far assigned target pins nothing: the walker hunts") {
    assert(hunt(hasTarget = true, targetBeyondPursuit = true))
  }

  test("a ghost past sight plus margin gets hunted, not posed at") {
    assert(hunt(hasTarget = true, targetVisible = false, targetBeyondSight = true))
  }

  test("sight-edge flicker keeps the line steady instead of flapping") {
    assert( ! hunt(hasTarget = true, targetVisible = false, targetBeyondSight = false))
  }
}
