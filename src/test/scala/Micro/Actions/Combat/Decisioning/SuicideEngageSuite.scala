package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class SuicideEngageSuite extends AnyFunSuite {

  test("a long-idle suicide unit charges the nearest enemy") {
    assert(SuicideEngage.shouldForceAttack(framesSinceAttack = 241, hasEnemy = true))
    assert(SuicideEngage.shouldForceAttack(framesSinceAttack = 4000, hasEnemy = true))
  }

  test("recent fire or no enemy vetoes the charge") {
    assert( ! SuicideEngage.shouldForceAttack(framesSinceAttack = 240, hasEnemy = true))
    assert( ! SuicideEngage.shouldForceAttack(framesSinceAttack = 241, hasEnemy = false))
  }
}
