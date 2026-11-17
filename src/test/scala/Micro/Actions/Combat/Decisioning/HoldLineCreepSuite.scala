package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class HoldLineCreepSuite extends AnyFunSuite {

  test("a dressed ranged line creeps, nothing else does") {
    assert(HoldLineCreep.shouldCreep(hasFormation = true, melee = false, weaponReady = true, fractionInSlot = 1.0, beyondRangeTiles = 5.0))
    assert(HoldLineCreep.shouldCreep(hasFormation = true, melee = false, weaponReady = true, fractionInSlot = 0.8, beyondRangeTiles = 0.1))
    assert( ! HoldLineCreep.shouldCreep(hasFormation = false, melee = false, weaponReady = true, fractionInSlot = 1.0, beyondRangeTiles = 5.0))
    assert( ! HoldLineCreep.shouldCreep(hasFormation = true, melee = true, weaponReady = true, fractionInSlot = 1.0, beyondRangeTiles = 5.0))
    assert( ! HoldLineCreep.shouldCreep(hasFormation = true, melee = false, weaponReady = false, fractionInSlot = 1.0, beyondRangeTiles = 5.0))
    assert( ! HoldLineCreep.shouldCreep(hasFormation = true, melee = false, weaponReady = true, fractionInSlot = 0.79, beyondRangeTiles = 5.0))
    assert( ! HoldLineCreep.shouldCreep(hasFormation = true, melee = false, weaponReady = true, fractionInSlot = 1.0, beyondRangeTiles = 0.0))
  }

  test("steps are a tenth of a tile and stop at the target") {
    assert(HoldLineCreep.stepTiles(5.0) == 0.1)
    assert(HoldLineCreep.stepTiles(0.05) == 0.05)
    assert(HoldLineCreep.stepTiles(0.0) == 0.0)
  }
}
