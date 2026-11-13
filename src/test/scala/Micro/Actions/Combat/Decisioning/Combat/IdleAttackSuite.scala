package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class IdleAttackSuite extends AnyFunSuite {

  test("an idle warrior with a free kill shoots") {
    assert(IdleAttack.shouldForceAttack(framesSinceAttack = 151, hasHittableEnemy = true, shouldFight = true, locallyAhead = true))
    assert(IdleAttack.shouldForceAttack(framesSinceAttack = 4000, hasHittableEnemy = true, shouldFight = true, locallyAhead = true))
  }

  test("recent fire, no target, retreat or pressure all veto") {
    assert( ! IdleAttack.shouldForceAttack(framesSinceAttack = 150, hasHittableEnemy = true, shouldFight = true, locallyAhead = true))
    assert( ! IdleAttack.shouldForceAttack(framesSinceAttack = 151, hasHittableEnemy = false, shouldFight = true, locallyAhead = true))
    assert( ! IdleAttack.shouldForceAttack(framesSinceAttack = 151, hasHittableEnemy = true, shouldFight = false, locallyAhead = true))
    assert( ! IdleAttack.shouldForceAttack(framesSinceAttack = 151, hasHittableEnemy = true, shouldFight = true, locallyAhead = false))
  }
}
