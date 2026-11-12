package Tactic.Squads.defense

import org.scalatest.funsuite.AnyFunSuite

class SquadBaseGuardsSuite extends AnyFunSuite {

  test("a far enemy never trips the guard") {
    assert( ! BaseGuardFight.shouldEngage(30.01))
    assert( ! BaseGuardFight.shouldEngage(200.0))
  }

  test("an enemy inside thirty ground tiles trips the guard") {
    assert(BaseGuardFight.shouldEngage(30.0))
    assert(BaseGuardFight.shouldEngage(0.0))
  }
}
