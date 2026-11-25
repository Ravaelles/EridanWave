package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class EnemyWallInDetectedSuite extends AnyFunSuite {

  test("chew the depot down to 31 red hit points, no lower") {
    assert(EnemyWallInDetected.shouldFightDepot(200))
    assert(EnemyWallInDetected.shouldFightDepot(31))
    assert( ! EnemyWallInDetected.shouldFightDepot(30))
  }

  test("fresh shields trade into the wall: wound at or below 13") {
    assert(EnemyWallInDetected.attackShieldWound == 13)
  }

  test("without tanks the wall is always taken, whatever the wound") {
    assert(EnemyWallInDetected.shouldTakeWall(enemyHasTanks = false, shieldWound = 0))
    assert(EnemyWallInDetected.shouldTakeWall(enemyHasTanks = false, shieldWound = 13))
    assert(EnemyWallInDetected.shouldTakeWall(enemyHasTanks = false, shieldWound = 80))
  }

  test("under tanks only fresh shields trade: wound 13 attacks, 14 yields") {
    assert(EnemyWallInDetected.shouldTakeWall(enemyHasTanks = true, shieldWound = 0))
    assert(EnemyWallInDetected.shouldTakeWall(enemyHasTanks = true, shieldWound = 13))
    assert( ! EnemyWallInDetected.shouldTakeWall(enemyHasTanks = true, shieldWound = 14))
    assert( ! EnemyWallInDetected.shouldTakeWall(enemyHasTanks = true, shieldWound = 80))
  }

  test("the pack chews one shared depot: nearest to its centroid") {
    val a = (101, 0.0)
    val b = (102, 10.0)
    assert(EnemyWallInDetected.pickDepot(Seq(a, b)).contains(101))
    assert(EnemyWallInDetected.pickDepot(Seq(b, a)).contains(101))
    assert(EnemyWallInDetected.pickDepot(Seq.empty[(Int, Double)]).isEmpty)
  }
}
