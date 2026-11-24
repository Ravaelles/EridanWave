package Micro.Actions.Combat.Decisioning

import Utilities.Time.Minutes
import bwapi.UnitType
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class HuntBehaviorSuite extends AnyFunSuite {

  private def hunt(
    umsMode             : Boolean = true,
    hasVisibleEnemies   : Boolean = false,
    isWarrior           : Boolean = true,
    isReaver            : Boolean = false,
    canMove             : Boolean = true,
    hasTarget           : Boolean = false,
    targetVisible       : Boolean = true,
    targetBeyondPursuit : Boolean = false,
    targetBeyondSight   : Boolean = false,
    verdictDuel         : Boolean = false,
    dressHeld           : Boolean = false): Boolean =
    HuntBehavior.shouldHunt(
      umsMode,
      hasVisibleEnemies,
      isWarrior,
      isReaver,
      canMove,
      hasTarget,
      targetVisible,
      targetBeyondPursuit,
      targetBeyondSight,
      verdictDuel,
      dressHeld)

  test("a blind walker with no target mops up") {
    assert(hunt())
  }

  test("any visible enemy warrior owns the frame: no ghost hunt beside it") {
    assert( ! hunt(hasVisibleEnemies = true))
    assert( ! hunt(hasVisibleEnemies = true, hasTarget = true, targetBeyondPursuit = true))
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

  test("a latched duel pins the hull: no memory hunt beside a live duel") {
    assert( ! hunt(verdictDuel = true))
    assert( ! hunt(hasTarget = true, targetBeyondPursuit = true, verdictDuel = true))
  }

  test("a dressed line holds instead of hunting through the wait") {
    assert( ! hunt(dressHeld = true))
    assert( ! hunt(hasTarget = true, targetBeyondPursuit = true, dressHeld = true))
  }

  test("a fresh mobile contact is remembered") {
    assert(HuntBehavior.remembersMobile(Minutes(3)() - 1))
  }

  test("a three-minute-old mobile contact is forgotten") {
    assert( ! HuntBehavior.remembersMobile(Minutes(3)()))
  }

  test("only mobile warriors hunt, never Reavers or the immobile") {
    TestKit.seedWorld()
    assert(HuntBehavior.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert(HuntBehavior.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 2)))
    assert( ! HuntBehavior.appliesToUnit(TestKit.friendly(UnitType.Protoss_Probe, 3)))
    assert( ! HuntBehavior.appliesToUnit(TestKit.friendly(UnitType.Protoss_Reaver, 4)))
    assert( ! HuntBehavior.appliesToUnit(TestKit.friendly(UnitType.Protoss_Pylon, 5)))
  }
}
