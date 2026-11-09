package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class SquadMarchSuite extends AnyFunSuite {

  test("fight consensus alone forms the march") {
    assert(SquadMarch.shouldFormMarch(fightConsensus = true, hasEnemiesEnRoute = false))
  }

  test("enemies en route form the march before any vote to fight") {
    assert(SquadMarch.shouldFormMarch(fightConsensus = false, hasEnemiesEnRoute = true))
  }

  test("no vote and no enemies mean no march formation") {
    assert( ! SquadMarch.shouldFormMarch(fightConsensus = false, hasEnemiesEnRoute = false))
  }
}
