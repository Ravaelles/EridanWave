package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

/**
 * Yield-cap rows: the step out never exceeds the walk-back inside the
 * remaining cooldown. Big cooldowns keep the full wound want, small
 * ones shorten the step, and inside reaction the cap plants at zero.
 */
class KiteYieldSuite extends AnyFunSuite {

  test("long cooldowns keep the full wound want") {
    assert(KiteYield.offsetPixels(woundWantPixels = 45.0, cooldownLeft = 30, topSpeedPixels = 2.5, reactionFrames = 4) == 45.0)
  }

  test("short cooldowns shorten the step to the walk-back") {
    assert(KiteYield.offsetPixels(woundWantPixels = 45.0, cooldownLeft = 10, topSpeedPixels = 2.5, reactionFrames = 4) == 15.0)
  }

  test("inside reaction the cap plants at zero") {
    assert(KiteYield.offsetPixels(woundWantPixels = 45.0, cooldownLeft = 4, topSpeedPixels = 2.5, reactionFrames = 4) == 0.0)
    assert(KiteYield.offsetPixels(woundWantPixels = 45.0, cooldownLeft = 0, topSpeedPixels = 2.5, reactionFrames = 4) == 0.0)
  }
}
