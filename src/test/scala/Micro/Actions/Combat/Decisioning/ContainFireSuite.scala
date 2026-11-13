package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class ContainFireSuite extends AnyFunSuite {

  test("a ready weapon shoots during the contain") {
    assert(ContainFire.shouldShoot(containing = true, cooldownLeft = 0, hasHittableTarget = true))
  }

  test("cooldown, no target or no contain hold the fire") {
    assert( ! ContainFire.shouldShoot(containing = true, cooldownLeft = 1, hasHittableTarget = true))
    assert( ! ContainFire.shouldShoot(containing = true, cooldownLeft = 0, hasHittableTarget = false))
    assert( ! ContainFire.shouldShoot(containing = false, cooldownLeft = 0, hasHittableTarget = true))
  }
}
