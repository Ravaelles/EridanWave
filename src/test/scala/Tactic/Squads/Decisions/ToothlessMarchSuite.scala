package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class ToothlessMarchSuite extends AnyFunSuite {

  test("combat units march straight at a toothless Terran") {
    assert(ToothlessMarch.shouldForceAttack(hasWarrior = true, enemyIsTerran = true, enemyHasTanks = false, enemyHasVultures = false))
    assert( ! ToothlessMarch.shouldForceAttack(hasWarrior = false, enemyIsTerran = true, enemyHasTanks = false, enemyHasVultures = false))
    assert( ! ToothlessMarch.shouldForceAttack(hasWarrior = true, enemyIsTerran = false, enemyHasTanks = false, enemyHasVultures = false))
    assert( ! ToothlessMarch.shouldForceAttack(hasWarrior = true, enemyIsTerran = true, enemyHasTanks = true, enemyHasVultures = false))
    assert( ! ToothlessMarch.shouldForceAttack(hasWarrior = true, enemyIsTerran = true, enemyHasTanks = false, enemyHasVultures = true))
  }
}
