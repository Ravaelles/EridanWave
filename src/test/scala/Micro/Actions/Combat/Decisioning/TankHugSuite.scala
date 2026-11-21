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

  test("a blade on the tank never withdraws: point-blank answers nothing") {
    assert(TankHug.holdsTank(distTiles = 1.1, isMelee = true))
    assert(TankHug.holdsTank(distTiles = 0.0, isMelee = true))
    assert( ! TankHug.holdsTank(distTiles = 1.11, isMelee = true))
    assert( ! TankHug.holdsTank(distTiles = 0.0, isMelee = false))
  }
}
