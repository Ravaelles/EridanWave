package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class HoldLineAdvanceSuite extends AnyFunSuite {

  test("a dressed ranged line creeps, nothing else does") {
    assert(HoldLineAdvance.shouldAdvance(hasFormation = true, melee = false, weaponReady = true, fractionInSlot = 1.0, beyondRangeTiles = 5.0))
    assert(HoldLineAdvance.shouldAdvance(hasFormation = true, melee = false, weaponReady = true, fractionInSlot = 0.8, beyondRangeTiles = 0.1))
    assert( ! HoldLineAdvance.shouldAdvance(hasFormation = false, melee = false, weaponReady = true, fractionInSlot = 1.0, beyondRangeTiles = 5.0))
    assert( ! HoldLineAdvance.shouldAdvance(hasFormation = true, melee = true, weaponReady = true, fractionInSlot = 1.0, beyondRangeTiles = 5.0))
    assert( ! HoldLineAdvance.shouldAdvance(hasFormation = true, melee = false, weaponReady = false, fractionInSlot = 1.0, beyondRangeTiles = 5.0))
    assert( ! HoldLineAdvance.shouldAdvance(hasFormation = true, melee = false, weaponReady = true, fractionInSlot = 0.79, beyondRangeTiles = 5.0))
    assert( ! HoldLineAdvance.shouldAdvance(hasFormation = true, melee = false, weaponReady = true, fractionInSlot = 1.0, beyondRangeTiles = 0.0))
  }

  test("steps are a tenth of a tile and stop at the target") {
    assert(HoldLineAdvance.stepTiles(5.0) == 0.1)
    assert(HoldLineAdvance.stepTiles(0.05) == 0.05)
    assert(HoldLineAdvance.stepTiles(0.0) == 0.0)
  }

  test("the step scales with the odds inside the courage band") {
    assert(HoldLineAdvance.advanceTilesFor(usSkim = 100.0, foeSkim = 100.0) == 0.1)
    assert(HoldLineAdvance.advanceTilesFor(usSkim = 200.0, foeSkim = 100.0) == 0.2)
    assert(HoldLineAdvance.advanceTilesFor(usSkim = 800.0, foeSkim = 100.0) == 0.2)
    assert(HoldLineAdvance.advanceTilesFor(usSkim = 50.0, foeSkim = 100.0) == 0.05)
    assert(HoldLineAdvance.advanceTilesFor(usSkim = 10.0, foeSkim = 100.0) == 0.05)
    assert(HoldLineAdvance.advanceTilesFor(usSkim = 100.0, foeSkim = 0.0) == 0.1)
  }
}
