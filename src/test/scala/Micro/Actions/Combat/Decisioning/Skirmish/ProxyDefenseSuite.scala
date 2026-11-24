package Micro.Actions.Combat.Decisioning.Skirmish

import org.scalatest.funsuite.AnyFunSuite

class ProxyDefenseSuite extends AnyFunSuite {

  test("a thin home line stands when the enemy is already inside") {
    assert(ProxyDefense.maxDefenders == 15)
    assert(ProxyDefense.holdsGround(enemyIsProtoss = true, ourWarriors = 15, nearestFoeMainTiles = 11.9, homeRadiusTiles = 12.0))
    assert(ProxyDefense.holdsGround(enemyIsProtoss = true, ourWarriors = 0, nearestFoeMainTiles = 0.0, homeRadiusTiles = 12.0))
  }

  test("no proxy, no stand: other matchups, full counts and far foes march as before") {
    assert( ! ProxyDefense.holdsGround(enemyIsProtoss = false, ourWarriors = 15, nearestFoeMainTiles = 11.9, homeRadiusTiles = 12.0))
    assert( ! ProxyDefense.holdsGround(enemyIsProtoss = true, ourWarriors = 16, nearestFoeMainTiles = 11.9, homeRadiusTiles = 12.0))
    assert( ! ProxyDefense.holdsGround(enemyIsProtoss = true, ourWarriors = 15, nearestFoeMainTiles = 12.0, homeRadiusTiles = 12.0))
    assert( ! ProxyDefense.holdsGround(enemyIsProtoss = true, ourWarriors = 15, nearestFoeMainTiles = 99.0, homeRadiusTiles = 12.0))
  }
}
