package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class TargetScoringSuite extends AnyFunSuite {

  test("air sizes route to air damages") {
    assert(TargetScoring.damageForSize(true, 2, 10, 20, 30, 40, 50, 60) == 10)
    assert(TargetScoring.damageForSize(true, 1, 10, 20, 30, 40, 50, 60) == 20)
    assert(TargetScoring.damageForSize(true, 0, 10, 20, 30, 40, 50, 60) == 30)
  }

  test("ground sizes route to ground damages") {
    assert(TargetScoring.damageForSize(false, 2, 10, 20, 30, 40, 50, 60) == 40)
    assert(TargetScoring.damageForSize(false, 1, 10, 20, 30, 40, 50, 60) == 50)
    assert(TargetScoring.damageForSize(false, 0, 10, 20, 30, 40, 50, 60) == 60)
  }

  test("morphing Creep Colony outranks idle, idle outranks ordinary targets") {
    assert(TargetScoring.creepColonyBonus(isCreepColony = true, morphing = true) == 14.0)
    assert(TargetScoring.creepColonyBonus(isCreepColony = true, morphing = false) == 8.0)
    assert(TargetScoring.creepColonyBonus(isCreepColony = false, morphing = true) == 0.0)
    assert(TargetScoring.creepColonyBonus(isCreepColony = false, morphing = false) == 0.0)
  }

  test("lone far Ling baits nothing while buildings stand") {
    assert(TargetScoring.loneLingLurePenalty(isLing = true, packNear = false, farFromHome = true, betterTargets = true) == -10000.0)
    assert(TargetScoring.loneLingLurePenalty(isLing = true, packNear = true, farFromHome = true, betterTargets = true) == 0.0)
    assert(TargetScoring.loneLingLurePenalty(isLing = true, packNear = false, farFromHome = false, betterTargets = true) == 0.0)
    assert(TargetScoring.loneLingLurePenalty(isLing = true, packNear = false, farFromHome = true, betterTargets = false) == 0.0)
    assert(TargetScoring.loneLingLurePenalty(isLing = false, packNear = false, farFromHome = true, betterTargets = true) == 0.0)
  }
}
