package Micro.Actions.Combat.Spells.Protoss

import org.scalatest.funsuite.AnyFunSuite

class PsionicStormSuite extends AnyFunSuite {

  test("two own units may share a storm") {
    assert( ! PsionicStorm.vetoFriendlyHits(0))
    assert( ! PsionicStorm.vetoFriendlyHits(2))
  }

  test("three own units veto the storm") {
    assert(PsionicStorm.vetoFriendlyHits(3))
    assert(PsionicStorm.vetoFriendlyHits(12))
  }

  test("Overlords never eat storms off a full battery") {
    assert(PsionicStorm.vetoOverlord(isOverlord = true, energy = 0))
    assert(PsionicStorm.vetoOverlord(isOverlord = true, energy = 194))
    assert( ! PsionicStorm.vetoOverlord(isOverlord = true, energy = 195))
    assert( ! PsionicStorm.vetoOverlord(isOverlord = false, energy = 0))
  }

  test("a full battery storms anything, a dry one only storms a pack") {
    assert(PsionicStorm.minPackEnemies(energy = 200, enemyIsProtoss = false) == 1)
    assert(PsionicStorm.minPackEnemies(energy = 180, enemyIsProtoss = true) == 1)
    assert(PsionicStorm.minPackEnemies(energy = 179, enemyIsProtoss = false) == 2)
    assert(PsionicStorm.minPackEnemies(energy = 100, enemyIsProtoss = true) == 2)
    assert(PsionicStorm.minPackEnemies(energy = 99, enemyIsProtoss = true) == 3)
    assert(PsionicStorm.minPackEnemies(energy = 99, enemyIsProtoss = false) == 4)
    assert(PsionicStorm.minPackEnemies(energy = 75, enemyIsProtoss = false) == 4)
    assert(PsionicStorm.minPackEnemies(energy = 74, enemyIsProtoss = false) == Int.MaxValue)
    assert(PsionicStorm.minPackEnemies(energy = 0, enemyIsProtoss = true) == Int.MaxValue)
  }

  test("crucial targets bypass the pack rule, thin packs never cast") {
    assert( ! PsionicStorm.vetoThinPack(foeCount = 1, hasCrucialTarget = true, energy = 75, enemyIsProtoss = false))
    assert(PsionicStorm.vetoThinPack(foeCount = 1, hasCrucialTarget = false, energy = 75, enemyIsProtoss = false))
    assert(PsionicStorm.vetoThinPack(foeCount = 2, hasCrucialTarget = false, energy = 119, enemyIsProtoss = false))
    assert( ! PsionicStorm.vetoThinPack(foeCount = 3, hasCrucialTarget = false, energy = 120, enemyIsProtoss = false))
    assert(PsionicStorm.vetoThinPack(foeCount = 40, hasCrucialTarget = false, energy = 74, enemyIsProtoss = false))
  }

  test("storms lead their victims two tiles") {
    assert(PsionicStorm.stormLeadTiles == 2.0)
  }

  test("storm walks short: twelve tiles vs Terran, ten otherwise") {
    assert(PsionicStorm.maxApproachVsTerran(isTerran = true) == 12)
    assert(PsionicStorm.maxApproachVsTerran(isTerran = false) == 10)
  }
}
