package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

class HoldToShootSuite extends AnyFunSuite {

  test("a runner never holds") {
    assert( ! HoldToShoot.shouldStand(gapPx = 100, effectiveRangePx = 128, cooldownLeft = 0, latencyFrames = 3, closingPxPerFrame = 0, holdMarginFrames = 5))
    assert( ! HoldToShoot.shouldStand(gapPx = 100, effectiveRangePx = 128, cooldownLeft = 0, latencyFrames = 3, closingPxPerFrame = -2, holdMarginFrames = 5))
  }

  test("a ready gun waits for a later arrival") {
    assert(HoldToShoot.shouldStand(gapPx = 300, effectiveRangePx = 128, cooldownLeft = 0, latencyFrames = 3, closingPxPerFrame = 4, holdMarginFrames = 5))
  }

  test("a gun ready past arrival keeps kiting") {
    assert( ! HoldToShoot.shouldStand(gapPx = 200, effectiveRangePx = 128, cooldownLeft = 30, latencyFrames = 3, closingPxPerFrame = 4, holdMarginFrames = 5))
  }

  test("inside effective range a ready gun holds, a cooling gun kites") {
    assert(HoldToShoot.shouldStand(gapPx = 100, effectiveRangePx = 128, cooldownLeft = 0, latencyFrames = 0, closingPxPerFrame = 4, holdMarginFrames = 5))
    assert( ! HoldToShoot.shouldStand(gapPx = 100, effectiveRangePx = 128, cooldownLeft = 8, latencyFrames = 0, closingPxPerFrame = 4, holdMarginFrames = 5))
  }

  test("effective range grows with windup, shrinks with braking, floors at zero") {
    assert(HoldToShoot.effectiveRange(foeReachPx = 32, foeSpeedPxPerFrame = 4, windupFrames = 5, brakingPx = 10) == 42)
    assert(HoldToShoot.effectiveRange(foeReachPx = 32, foeSpeedPxPerFrame = 4, windupFrames = 0, brakingPx = 100) == 0)
  }

  test("a nearly-ready gun shoots a Vulture in range instead of kiting") {
    // Dragoon reach 128: inside it with cooldown 5 the Vulture eats steel.
    assert(HoldToShoot.shouldStandVsVulture(100, 128, 5, 3, 4, foeFacingUs = true))
    assert(HoldToShoot.shouldStandVsVulture(100, 128, 0, 3, 4, foeFacingUs = false))
  }

  test("a cooling gun keeps kiting a Vulture it cannot punish") {
    assert( ! HoldToShoot.shouldStandVsVulture(100, 128, 6, 3, 4, foeFacingUs = false))
  }

  test("a head-on Vulture meets the shot at bonus reach") {
    // 1.5x reach is 192: closing fast with a hot gun holds from 260 out,
    // and point-blank at the bonus edge with no latency holds too - but a
    // runner or a sideways Vulture never does.
    assert(HoldToShoot.shouldStandVsVulture(260, 128, 0, 3, 8, foeFacingUs = true))
    assert(HoldToShoot.shouldStandVsVulture(190, 128, 0, 0, 8, foeFacingUs = true))
    assert( ! HoldToShoot.shouldStandVsVulture(190, 128, 0, 3, 8, foeFacingUs = false))
    assert( ! HoldToShoot.shouldStandVsVulture(190, 128, 0, 3, 0, foeFacingUs = true))
  }
}
