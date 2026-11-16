package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class DragoonTerranStandSuite extends AnyFunSuite {

  private def stands(
    shieldWound: Int = 0,
    cooldownLeft: Int = 0,
    terranInRange: Boolean = true): Boolean =
    DragoonTerranStand.shouldStand(shieldWound, cooldownLeft, terranInRange)

  test("a fresh hot gun shoots instead of running") {
    assert(stands())
  }

  test("a chewed hull still runs") {
    assert( ! stands(shieldWound = DragoonTerranStand.maxShieldWound + 1))
    assert(stands(shieldWound = DragoonTerranStand.maxShieldWound))
  }

  test("a cold gun still kites") {
    assert( ! stands(cooldownLeft = DragoonTerranStand.readyCooldownFrames + 1))
    assert(stands(cooldownLeft = DragoonTerranStand.readyCooldownFrames))
  }

  test("nothing Terran in range means nothing to stand for") {
    assert( ! stands(terranInRange = false))
  }
}
