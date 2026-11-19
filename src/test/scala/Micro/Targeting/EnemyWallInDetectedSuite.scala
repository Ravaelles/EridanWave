package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class EnemyWallInDetectedSuite extends AnyFunSuite {

  test("chew the depot down to 31 red hit points, no lower") {
    assert(EnemyWallInDetected.shouldFightDepot(200))
    assert(EnemyWallInDetected.shouldFightDepot(31))
    assert( ! EnemyWallInDetected.shouldFightDepot(30))
  }

  test("tanks plus ten or fewer shields yield the wall") {
    assert(EnemyWallInDetected.shouldYieldToTanks(enemyHasTanks = true, shields = 10))
    assert(EnemyWallInDetected.shouldYieldToTanks(enemyHasTanks = true, shields = 0))
    assert( ! EnemyWallInDetected.shouldYieldToTanks(enemyHasTanks = true, shields = 11))
    assert( ! EnemyWallInDetected.shouldYieldToTanks(enemyHasTanks = false, shields = 0))
  }

  test("the pack chews one shared depot: nearest to its centroid") {
    val a = (101, 0.0)
    val b = (102, 10.0)
    assert(EnemyWallInDetected.pickDepot(Seq(a, b)).contains(101))
    assert(EnemyWallInDetected.pickDepot(Seq(b, a)).contains(101))
    assert(EnemyWallInDetected.pickDepot(Seq.empty[(Int, Double)]).isEmpty)
  }

  test("ranged hulls hold inside eight tiles instead of marching in") {
    assert(EnemyWallInDetected.shouldHoldVsWall(7.9))
    assert(EnemyWallInDetected.shouldHoldVsWall(8.0))
    assert( ! EnemyWallInDetected.shouldHoldVsWall(8.1))
  }
}
