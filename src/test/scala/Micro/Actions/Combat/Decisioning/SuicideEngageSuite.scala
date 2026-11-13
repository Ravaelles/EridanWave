package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class SuicideEngageSuite extends AnyFunSuite {

  test("a long-idle suicide mission charges the nearest enemy") {
    assert(SuicideEngage.shouldForceAttack(framesSinceAttack = 241, hasEnemy = true, isSuicideMission = true))
    assert(SuicideEngage.shouldForceAttack(framesSinceAttack = 4000, hasEnemy = true, isSuicideMission = true))
  }

  test("recent fire, no enemy or no mission vetoes the charge") {
    assert( ! SuicideEngage.shouldForceAttack(framesSinceAttack = 240, hasEnemy = true, isSuicideMission = true))
    assert( ! SuicideEngage.shouldForceAttack(framesSinceAttack = 241, hasEnemy = false, isSuicideMission = true))
    assert( ! SuicideEngage.shouldForceAttack(framesSinceAttack = 241, hasEnemy = true, isSuicideMission = false))
  }
}
