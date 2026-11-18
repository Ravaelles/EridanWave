package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class NoTankAttackSuite extends AnyFunSuite {

  test("dragoons force the attack on a tankless Terran") {
    assert(NoTankAttack.shouldForceAttack(hasDragoon = true, enemyIsTerran = true, enemyHasTanks = false))
    assert( ! NoTankAttack.shouldForceAttack(hasDragoon = false, enemyIsTerran = true, enemyHasTanks = false))
    assert( ! NoTankAttack.shouldForceAttack(hasDragoon = true, enemyIsTerran = false, enemyHasTanks = false))
    assert( ! NoTankAttack.shouldForceAttack(hasDragoon = true, enemyIsTerran = true, enemyHasTanks = true))
  }
}
