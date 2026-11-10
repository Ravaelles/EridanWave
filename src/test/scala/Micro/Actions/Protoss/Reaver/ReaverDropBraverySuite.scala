package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverDropBraverySuite extends AnyFunSuite {

  test("a healthy pair drops onto at most two enemies") {
    assert(ReaverDropBravery.shouldDrop(enemyCount = 0, shuttleShields = 60, reaverShields = 80))
    assert(ReaverDropBravery.shouldDrop(enemyCount = 1, shuttleShields = 50, reaverShields = 50))
    assert(ReaverDropBravery.shouldDrop(enemyCount = 2, shuttleShields = 60, reaverShields = 60))
  }

  test("a third enemy keeps the Shuttle flying") {
    assert( ! ReaverDropBravery.shouldDrop(enemyCount = 3, shuttleShields = 60, reaverShields = 80))
  }

  test("either hull below fifty shields vetoes the drop") {
    assert( ! ReaverDropBravery.shouldDrop(enemyCount = 1, shuttleShields = 49, reaverShields = 80))
    assert( ! ReaverDropBravery.shouldDrop(enemyCount = 1, shuttleShields = 60, reaverShields = 49))
  }

  test("vs Terran a pair of tanks still allows the drop") {
    assert(ReaverDropBravery.shouldDrop(enemyCount = 1, shuttleShields = 60, reaverShields = 60, isTerran = true, enemyTanks = 0))
    assert(ReaverDropBravery.shouldDrop(enemyCount = 2, shuttleShields = 60, reaverShields = 60, isTerran = true, enemyTanks = 2))
  }

  test("vs Terran a third tank keeps the Shuttle flying") {
    assert( ! ReaverDropBravery.shouldDrop(enemyCount = 1, shuttleShields = 60, reaverShields = 60, isTerran = true, enemyTanks = 3))
  }

  test("other matchups ignore the tank count") {
    assert(ReaverDropBravery.shouldDrop(enemyCount = 2, shuttleShields = 60, reaverShields = 60, isTerran = false, enemyTanks = 9))
  }

  test("a lone Hydralisk cannot outdamage a healthy Shuttle") {
    // Hydralisk ~0.46 dps vs air; a full Shuttle holds 140: trivially affordable
    assert(ReaverDropBravery.canAffordShot(ourHitPoints = 140.0, enemyDps = 0.5))
  }

  test("a real firing squad refuses the drop") {
    assert( ! ReaverDropBravery.canAffordShot(ourHitPoints = 140.0, enemyDps = 5.0))
    assert( ! ReaverDropBravery.canAffordShot(ourHitPoints = 60.0, enemyDps = 0.5))
  }

  test("expected damage at the margin boundary holds") {
    // 2 dps over 72 frames doubled is 288: 289 fits, 288 does not
    assert(ReaverDropBravery.canAffordShot(ourHitPoints = 289.0, enemyDps = 2.0))
    assert( ! ReaverDropBravery.canAffordShot(ourHitPoints = 288.0, enemyDps = 2.0))
  }
}
