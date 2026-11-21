package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class TankHugSuite extends AnyFunSuite {

  test("a cooling warrior hugs a close sieged tank") {
    assert(TankHug.shouldHug(siegedTankTiles = 6.9, cooldownLeft = 8, shouldFight = true, canMove = true))
    assert(TankHug.shouldHug(siegedTankTiles = 0.2, cooldownLeft = 30, shouldFight = true, canMove = true))
  }

  test("far tanks, ready guns, routers and the immobile never hug") {
    assert( ! TankHug.shouldHug(siegedTankTiles = 7.01, cooldownLeft = 8, shouldFight = true, canMove = true))
    assert( ! TankHug.shouldHug(siegedTankTiles = 6.9, cooldownLeft = 7, shouldFight = true, canMove = true))
    assert( ! TankHug.shouldHug(siegedTankTiles = 6.9, cooldownLeft = 8, shouldFight = false, canMove = true))
    assert( ! TankHug.shouldHug(siegedTankTiles = 6.9, cooldownLeft = 8, shouldFight = true, canMove = false))
  }

  test("hugging stops at touching distance") {
    assert(TankHug.closeEnough(0.2))
    assert(TankHug.closeEnough(0.0))
    assert( ! TankHug.closeEnough(0.21))
  }

  test("fresh shells outside the hug break the duel off") {
    assert(TankHug.shouldWithdraw(shelledRecently = true, nearestSiegedTiles = 0.3, isMelee = false))
    assert(TankHug.shouldWithdraw(shelledRecently = true, nearestSiegedTiles = 12.0, isMelee = false))
  }

  test("a blade on the tank never withdraws: point-blank answers nothing") {
    assert(TankHug.holdsTank(distTiles = 1.1, isMelee = true))
    assert(TankHug.holdsTank(distTiles = 0.0, isMelee = true))
    assert( ! TankHug.holdsTank(distTiles = 1.11, isMelee = true))
    assert( ! TankHug.holdsTank(distTiles = 0.0, isMelee = false))
    assert( ! TankHug.shouldWithdraw(shelledRecently = true, nearestSiegedTiles = 1.1, isMelee = true))
    assert( ! TankHug.shouldWithdraw(shelledRecently = true, nearestSiegedTiles = 0.5, isMelee = true))
    assert(TankHug.shouldWithdraw(shelledRecently = true, nearestSiegedTiles = 1.11, isMelee = true))
  }

  test("stale shells and the hug itself hold the line") {
    assert( ! TankHug.shouldWithdraw(shelledRecently = false, nearestSiegedTiles = 12.0, isMelee = false))
    assert( ! TankHug.shouldWithdraw(shelledRecently = true, nearestSiegedTiles = 0.2, isMelee = false))
    assert( ! TankHug.shouldWithdraw(shelledRecently = true, nearestSiegedTiles = 0.0, isMelee = false))
  }

  test("inside the siege footprint the guns leave before the first shell") {
    assert(TankHug.tooCloseToSiege(0.0))
    assert(TankHug.tooCloseToSiege(12.49))
    assert( ! TankHug.tooCloseToSiege(12.5))
    assert( ! TankHug.tooCloseToSiege(20.0))
    assert( ! TankHug.tooCloseToSiege(Double.PositiveInfinity))
  }

  test("the leave releases two tiles past the footprint, never on the edge") {
    assert(TankHug.siegeReleaseTiles == 14.5)
    assert( ! TankHug.tooCloseToSiege(14.49))
  }

  test("a hot gun near siege walks out instead of holding under it") {
    assert(TankHug.shouldLeaveSiege(siegedTankTiles = 6.0, cooldownLeft = 0, shouldFight = true, canMove = true, isMelee = false))
    assert(TankHug.shouldLeaveSiege(siegedTankTiles = 12.49, cooldownLeft = 0, shouldFight = true, canMove = true, isMelee = false))
  }

  test("the cooling hug, the far line, routers and blades keep their rules") {
    assert( ! TankHug.shouldLeaveSiege(siegedTankTiles = 6.0, cooldownLeft = 8, shouldFight = true, canMove = true, isMelee = false))
    assert( ! TankHug.shouldLeaveSiege(siegedTankTiles = 12.5, cooldownLeft = 0, shouldFight = true, canMove = true, isMelee = false))
    assert( ! TankHug.shouldLeaveSiege(siegedTankTiles = 20.0, cooldownLeft = 0, shouldFight = true, canMove = true, isMelee = false))
    assert( ! TankHug.shouldLeaveSiege(siegedTankTiles = 6.0, cooldownLeft = 0, shouldFight = false, canMove = true, isMelee = false))
    assert( ! TankHug.shouldLeaveSiege(siegedTankTiles = 6.0, cooldownLeft = 0, shouldFight = true, canMove = false, isMelee = false))
    assert( ! TankHug.shouldLeaveSiege(siegedTankTiles = 6.0, cooldownLeft = 0, shouldFight = true, canMove = true, isMelee = true))
    assert( ! TankHug.shouldLeaveSiege(siegedTankTiles = Double.PositiveInfinity, cooldownLeft = 0, shouldFight = true, canMove = true, isMelee = false))
  }
}
