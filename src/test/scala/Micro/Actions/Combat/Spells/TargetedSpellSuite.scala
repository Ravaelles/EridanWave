package Micro.Actions.Combat.Spells

import org.scalatest.funsuite.AnyFunSuite

class TargetedSpellSuite extends AnyFunSuite {

  test("a fresh pick re-affirms an intent in the same fight") {
    assert(TargetedSpell.confirmSpot(distTiles = 0.0, confirmTiles = 2.0))
    assert(TargetedSpell.confirmSpot(distTiles = 2.0, confirmTiles = 2.0))
    assert( ! TargetedSpell.confirmSpot(distTiles = 2.01, confirmTiles = 2.0))
  }

  test("an unreachable intent goes stale after ten seconds") {
    assert( ! TargetedSpell.intentExpired(framesSinceIntent = 0, expiryFrames = 10 * 24))
    assert( ! TargetedSpell.intentExpired(framesSinceIntent = 10 * 24, expiryFrames = 10 * 24))
    assert(TargetedSpell.intentExpired(framesSinceIntent = 10 * 24 + 1, expiryFrames = 10 * 24))
  }
}
