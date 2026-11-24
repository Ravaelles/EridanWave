package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

class DragoonVsSlowlotSuite extends AnyFunSuite {

  test("shots of slack scale with hit points") {
    assert(DragoonVsSlowlot.hitsLeft(0) == 0)
    assert(DragoonVsSlowlot.hitsLeft(13) == 0)
    assert(DragoonVsSlowlot.hitsLeft(14) == 1)
    assert(DragoonVsSlowlot.hitsLeft(80) == 5)
    assert(DragoonVsSlowlot.hitsLeft(180) == 12)
  }

  test("safety adds tiles of margin to shots of slack") {
    assert(DragoonVsSlowlot.safety(marginPixels = 64, totalHealth = 14) == 2 + 1)
    assert(DragoonVsSlowlot.safety(marginPixels = 0, totalHealth = 0) == 0)
  }

  test("priority tiers follow safety: dying shoves, comfortable pardons") {
    assert(DragoonVsSlowlot.priorityTier(0) == 0)
    assert(DragoonVsSlowlot.priorityTier(2) == 0)
    assert(DragoonVsSlowlot.priorityTier(3) == 1)
    assert(DragoonVsSlowlot.priorityTier(5) == 2)
    assert(DragoonVsSlowlot.priorityTier(7) == 3)
  }

  test("margin required grows as the hull bleeds") {
    assert(DragoonVsSlowlot.marginRequiredPixels(180) == 32)
    assert(DragoonVsSlowlot.marginRequiredPixels(42) == 128 - 8 * 3)
    assert(DragoonVsSlowlot.marginRequiredPixels(0) == 128)
  }

  test("positive delta holds, negative runs") {
    assert(DragoonVsSlowlot.marginDeltaPixels(marginPixels = 200, totalHealth = 180) > 0)
    assert(DragoonVsSlowlot.marginDeltaPixels(marginPixels = 0, totalHealth = 180) < 0)
  }

  test("a targeted threat closing in carves the margin") {
    assert(DragoonVsSlowlot.threatMarginPixels(100.0, targeted = false, approachingDot = 0.0) == 132.0)
    assert(DragoonVsSlowlot.threatMarginPixels(100.0, targeted = true, approachingDot = 0.0) == 116.0)
    assert(DragoonVsSlowlot.threatMarginPixels(100.0, targeted = false, approachingDot = 1.0) == 116.0)
  }

  test("inside our own reach we walk out, loaded surplus steps in") {
    assert(DragoonVsSlowlot.insideOwnReach(-33.0))
    assert( ! DragoonVsSlowlot.insideOwnReach(-31.0))
    assert(DragoonVsSlowlot.shouldApproach(33))
    assert( ! DragoonVsSlowlot.shouldApproach(32))
  }
}
