package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class RepositionBehaviorSuite extends AnyFunSuite {

  test("a fresh gun pushes in, a cooling one hangs back") {
    assert(RepositionBehavior.urgency01(cooldownMax = 30, cooldownLeft = 0) == 1.0)
    assert(RepositionBehavior.urgency01(cooldownMax = 30, cooldownLeft = 15) == 0.5)
    assert(RepositionBehavior.urgency01(cooldownMax = 30, cooldownLeft = 30) == 0.0)
  }

  test("an overcooled gun never reverses the slide") {
    assert(RepositionBehavior.urgency01(cooldownMax = 30, cooldownLeft = 45) == 0.0)
  }

  test("a domain the hull cannot shoot never NaNs the forces") {
    assert(RepositionBehavior.urgency01(cooldownMax = 0, cooldownLeft = 0) == 1.0)
    assert( ! RepositionBehavior.urgency01(cooldownMax = 0, cooldownLeft = 0).isNaN)
  }

  test("the terminal volleys loaded hittable guns instead of shuffling") {
    assert(RepositionBehavior.shouldVolley(retreating = false, cooldownLeft = 0, hasHittable = true))
    assert( ! RepositionBehavior.shouldVolley(retreating = true, cooldownLeft = 0, hasHittable = true))
    assert( ! RepositionBehavior.shouldVolley(retreating = false, cooldownLeft = 8, hasHittable = true))
    assert( ! RepositionBehavior.shouldVolley(retreating = false, cooldownLeft = 0, hasHittable = false))
  }
}
