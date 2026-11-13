package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class NoTankMarchSuite extends AnyFunSuite {

  test("dragoons march straight at a tankless Terran") {
    assert(NoTankMarch.shouldMarchDirectly(isDragoon = true, enemyIsTerran = true, enemyHasTanks = false))
    assert( ! NoTankMarch.shouldMarchDirectly(isDragoon = false, enemyIsTerran = true, enemyHasTanks = false))
    assert( ! NoTankMarch.shouldMarchDirectly(isDragoon = true, enemyIsTerran = false, enemyHasTanks = false))
    assert( ! NoTankMarch.shouldMarchDirectly(isDragoon = true, enemyIsTerran = true, enemyHasTanks = true))
  }
}
