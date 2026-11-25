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

  test("at weight 4 a fresh adjacent target outranks a dying one five tiles out") {
    // Documents the shipped tradeoff: four tiles of range outweigh a
    // full wound, so a moving ball picks nearest and spreads.
    val freshNear = TargetScoring.attackValue(targetValue = 1.0, injury = 0.0, pixelCost = 0.0, efficacy = 1.0, injuryWeight = 4.0)
    val dyingFar = TargetScoring.attackValue(targetValue = 1.0, injury = 1.0, pixelCost = 5.0 * 32.0, efficacy = 1.0, injuryWeight = 4.0)
    assert(freshNear > dyingFar)
  }

  test("at weight 8 a dying target in reach outranks a fresh adjacent one") {
    // The experiment direction: a full wound buys eight tiles, so the
    // line finishes instead of spreading. Tune against UMS flips.
    val freshNear = TargetScoring.attackValue(targetValue = 1.0, injury = 0.0, pixelCost = 0.0, efficacy = 1.0, injuryWeight = 8.0)
    val dyingFar = TargetScoring.attackValue(targetValue = 1.0, injury = 1.0, pixelCost = 5.0 * 32.0, efficacy = 1.0, injuryWeight = 8.0)
    assert(dyingFar > freshNear)
  }

  test("lone far Ling baits nothing while buildings stand") {
    assert(TargetScoring.loneLingScatteredOnMapLurePenalty(isLing = true, packNear = false, farFromHome = true, betterTargets = true) == -10000.0)
    assert(TargetScoring.loneLingScatteredOnMapLurePenalty(isLing = true, packNear = true, farFromHome = true, betterTargets = true) == 0.0)
    assert(TargetScoring.loneLingScatteredOnMapLurePenalty(isLing = true, packNear = false, farFromHome = false, betterTargets = true) == 0.0)
    assert(TargetScoring.loneLingScatteredOnMapLurePenalty(isLing = true, packNear = false, farFromHome = true, betterTargets = false) == 0.0)
    assert(TargetScoring.loneLingScatteredOnMapLurePenalty(isLing = false, packNear = false, farFromHome = true, betterTargets = true) == 0.0)
  }
}
