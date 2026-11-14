package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class ContainFireSuite extends AnyFunSuite {

  test("a ready weapon shoots at a hittable target") {
    assert(ContainFire.shouldShoot(cooldownLeft = 0, hasHittableTarget = true))
  }

  test("cooldown or no target holds the fire") {
    assert( ! ContainFire.shouldShoot(cooldownLeft = 1, hasHittableTarget = true))
    assert( ! ContainFire.shouldShoot(cooldownLeft = 0, hasHittableTarget = false))
  }
}
