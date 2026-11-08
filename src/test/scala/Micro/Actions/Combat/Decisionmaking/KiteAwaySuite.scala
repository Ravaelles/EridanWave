package Micro.Actions.Combat.Decisionmaking

import org.scalatest.funsuite.AnyFunSuite

class KiteAwaySuite extends AnyFunSuite {

  test("wound levels follow total HP boundaries") {
    assert(KiteAway.isCriticallyWounded(25))
    assert( ! KiteAway.isCriticallyWounded(26))
    assert(KiteAway.isSeriouslyWounded(65))
    assert(KiteAway.isSeriouslyWounded(25))
    assert( ! KiteAway.isSeriouslyWounded(66))
  }

  test("offset grows linearly with injury up to the maximum") {
    assert(KiteAway.woundOffsetTiles(0.0) == 0.0)
    assert(KiteAway.woundOffsetTiles(0.5) == KiteAway.maxOffsetTiles / 2)
    assert(KiteAway.woundOffsetTiles(1.0) == KiteAway.maxOffsetTiles)
  }

  test("critical retreats until nearly ready, serious until almost ready") {
    assert(KiteAway.shouldRetreat(totalHealth = 20, woundFraction = 0.9, cooldownLeft = 5, enemyInMargin = true))
    assert( ! KiteAway.shouldRetreat(totalHealth = 20, woundFraction = 0.9, cooldownLeft = 4, enemyInMargin = true))
    assert(KiteAway.shouldRetreat(totalHealth = 60, woundFraction = 0.6, cooldownLeft = 11, enemyInMargin = true))
    assert( ! KiteAway.shouldRetreat(totalHealth = 60, woundFraction = 0.6, cooldownLeft = 10, enemyInMargin = true))
  }

  test("mildly wounded rotate on high cooldown, healthy hold") {
    assert(KiteAway.shouldRetreat(totalHealth = 120, woundFraction = 0.3, cooldownLeft = 16, enemyInMargin = true))
    assert( ! KiteAway.shouldRetreat(totalHealth = 120, woundFraction = 0.3, cooldownLeft = 15, enemyInMargin = true))
    assert( ! KiteAway.shouldRetreat(totalHealth = 170, woundFraction = 0.04, cooldownLeft = 22, enemyInMargin = true))
  }

  test("no retreat without an enemy pressing with margin") {
    assert( ! KiteAway.shouldRetreat(totalHealth = 20, woundFraction = 0.9, cooldownLeft = 22, enemyInMargin = false))
  }

  test("seriously wounded skip volley discipline") {
    assert(KiteAway.exemptFromVolley(60))
    assert(KiteAway.exemptFromVolley(20))
    assert( ! KiteAway.exemptFromVolley(66))
  }
}
