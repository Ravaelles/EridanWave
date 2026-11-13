package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverDropBraverySuite extends AnyFunSuite {

  test("ranged-threat classification counts only guns bearing on the Shuttle") {
    // Gun ranged bearing (Dragoon-like)
    assert(ReaverDropBravery.isRangedThreat(canAttack = true, melee = false, flying = false, canHitShuttle = true, isScourge = false))
    // Gun melee (Zealot-like): chews the landed Reaver, not the Shuttle
    assert( ! ReaverDropBravery.isRangedThreat(canAttack = true, melee = true, flying = false, canHitShuttle = false, isScourge = false))
    // Ground-only guns (Tank/Sunken-like): deadly below, harmless above
    assert( ! ReaverDropBravery.isRangedThreat(canAttack = true, melee = false, flying = false, canHitShuttle = false, isScourge = false))
    // Ground-only fliers (Guardian-like)
    assert( ! ReaverDropBravery.isRangedThreat(canAttack = true, melee = false, flying = true, canHitShuttle = false, isScourge = false))
    // Unarmed spotter (Overlord-like)
    assert( ! ReaverDropBravery.isRangedThreat(canAttack = false, melee = false, flying = true, canHitShuttle = false, isScourge = false))
    // Flying anti-air (Mutalisk-like)
    assert(ReaverDropBravery.isRangedThreat(canAttack = true, melee = false, flying = true, canHitShuttle = true, isScourge = false))
    // Scourge: no weapon in the data, kills Shuttles anyway
    assert(ReaverDropBravery.isRangedThreat(canAttack = false, melee = false, flying = true, canHitShuttle = false, isScourge = true))
    // Grounded Scourge-shaped edge: unarmed and down is no threat
    assert( ! ReaverDropBravery.isRangedThreat(canAttack = false, melee = false, flying = false, canHitShuttle = false, isScourge = true))
  }

  test("a healthy pair drops onto an empty landing zone") {
    assert(ReaverDropBravery.shouldDrop(foesClose = 0, rangedNear = 0, shuttleShields = 60, reaverShields = 80))
  }

  test("one foe inside six tiles still drops") {
    assert(ReaverDropBravery.shouldDrop(foesClose = 1, rangedNear = 1, shuttleShields = 50, reaverShields = 50))
  }

  test("two foes inside six tiles keep the Shuttle flying") {
    assert( ! ReaverDropBravery.shouldDrop(foesClose = 2, rangedNear = 0, shuttleShields = 60, reaverShields = 80))
  }

  test("two ranged inside eight tiles still drop, three refuse") {
    assert(ReaverDropBravery.shouldDrop(foesClose = 0, rangedNear = 2, shuttleShields = 60, reaverShields = 60))
    assert( ! ReaverDropBravery.shouldDrop(foesClose = 0, rangedNear = 3, shuttleShields = 60, reaverShields = 60))
  }

  test("either hull below fifty shields vetoes the drop") {
    assert( ! ReaverDropBravery.shouldDrop(foesClose = 1, rangedNear = 1, shuttleShields = 49, reaverShields = 80))
    assert( ! ReaverDropBravery.shouldDrop(foesClose = 1, rangedNear = 1, shuttleShields = 60, reaverShields = 49))
  }

  test("enemy ground statics veto everything") {
    assert( ! ReaverDropBravery.shouldDrop(foesClose = 0, rangedNear = 0, shuttleShields = 60, reaverShields = 80, nearStaticDefense = true))
  }

  test("vs Terran a pair of tanks still allows the drop") {
    assert(ReaverDropBravery.shouldDrop(foesClose = 1, rangedNear = 1, shuttleShields = 60, reaverShields = 60, isTerran = true, enemyTanks = 0))
    assert(ReaverDropBravery.shouldDrop(foesClose = 1, rangedNear = 1, shuttleShields = 60, reaverShields = 60, isTerran = true, enemyTanks = 2))
  }

  test("vs Terran a third tank keeps the Shuttle flying") {
    assert( ! ReaverDropBravery.shouldDrop(foesClose = 1, rangedNear = 1, shuttleShields = 60, reaverShields = 60, isTerran = true, enemyTanks = 3))
  }

  test("other matchups ignore the tank count") {
    assert(ReaverDropBravery.shouldDrop(foesClose = 1, rangedNear = 1, shuttleShields = 60, reaverShields = 60, isTerran = false, enemyTanks = 9))
  }

  test("a stale healthy Reaver gets the drop despite the rings") {
    assert(ReaverDropBravery.mayDropStale(framesSinceFired = 241, woundFraction = 0.1))
    assert(ReaverDropBravery.mayDropStale(framesSinceFired = 999999, woundFraction = 0.0))
  }

  test("a never-fired hull is not stale") {
    assert( ! ReaverDropBravery.mayDropStale(framesSinceFired = 999999, woundFraction = 0.0, hasFired = false))
  }

  test("melee inside three tiles vetoes the stale drop") {
    assert(ReaverDropBravery.mayDropStale(framesSinceFired = 500, woundFraction = 0.1, meleeNear = 0))
    assert( ! ReaverDropBravery.mayDropStale(framesSinceFired = 500, woundFraction = 0.1, meleeNear = 1))
  }

  test("a recently fired or hurt Reaver is not stale") {
    assert( ! ReaverDropBravery.mayDropStale(framesSinceFired = 240, woundFraction = 0.1))
    assert( ! ReaverDropBravery.mayDropStale(framesSinceFired = 500, woundFraction = 0.5))
    assert( ! ReaverDropBravery.mayDropStale(framesSinceFired = 100, woundFraction = 0.9))
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

  test("ring and theater thresholds are stored in tiles") {
    assert(ReaverDropBravery.closeDist == 6.0)
    assert(ReaverDropBravery.rangedDist == 8.0)
    assert(ReaverDropBravery.staticDefenseDist == 8.0)
    assert(ReaverDropBravery.tankTheaterTiles == 20.0)
  }

  private def facts(
    foesClose        : Int = 0,
    rangedNear       : Int = 0,
    rangedDps        : Double = 0.0,
    tanks            : Int = 0,
    nearStaticDefense: Boolean = false,
    meleeNear        : Int = 0,
    shuttleShields   : Int = 60,
    reaverShields    : Int = 80,
    shuttleHitPoints : Double = 140.0,
    firedAgo         : Int = 0,
    woundFraction    : Double = 0.0,
    hasFired         : Boolean = true,
    isTerran         : Boolean = false): ReaverDropBravery.DropFacts =
    ReaverDropBravery.DropFacts(
      foesClose, rangedNear, rangedDps, tanks, nearStaticDefense, meleeNear,
      shuttleShields, reaverShields, shuttleHitPoints, firedAgo, woundFraction, hasFired, isTerran)

  test("decide: a healthy pair over a quiet landing drops") {
    assert(ReaverDropBravery.decide(facts()))
  }

  test("decide: fire the pair cannot stomach vetoes the rings") {
    assert( ! ReaverDropBravery.decide(facts(rangedNear = 1, rangedDps = 5.0)))
    // The same shot is affordable for a full Shuttle.
    assert(ReaverDropBravery.decide(facts(rangedNear = 1, rangedDps = 0.5)))
  }

  test("decide: a static vetoes the stale rule as well as the rings") {
    assert( ! ReaverDropBravery.decide(facts(nearStaticDefense = true, firedAgo = 500, woundFraction = 0.1)))
  }

  test("decide: the stale rule rescues a blocked landing only when healthy") {
    // Three ranged guns refuse the rings; ten dry seconds still take the drop.
    val blocked = facts(rangedNear = 3, firedAgo = 500, woundFraction = 0.1)
    assert(ReaverDropBravery.decide(blocked))
    assert( ! ReaverDropBravery.decide(blocked.copy(meleeNear = 1)))
    assert( ! ReaverDropBravery.decide(blocked.copy(reaverShields = 40)))
    assert( ! ReaverDropBravery.decide(blocked.copy(shuttleShields = 40)))
    assert( ! ReaverDropBravery.decide(blocked.copy(hasFired = false)))
  }

  test("decide: the Terran tank gate covers both rules") {
    assert( ! ReaverDropBravery.decide(facts(isTerran = true, tanks = 3)))
    assert( ! ReaverDropBravery.decide(facts(isTerran = true, tanks = 3, firedAgo = 500, woundFraction = 0.1)))
    assert(ReaverDropBravery.decide(facts(isTerran = true, tanks = 2)))
  }
}
