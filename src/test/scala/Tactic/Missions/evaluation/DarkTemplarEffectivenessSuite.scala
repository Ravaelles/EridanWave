package Tactic.Missions.evaluation

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarEffectivenessSuite extends AnyFunSuite {

  test("the first four always build") {
    KillCredits.reset()
    assert(DarkTemplarEffectiveness.allows(produced = 0, alive = 0, gas = 0))
    assert(DarkTemplarEffectiveness.allows(produced = 3, alive = 3, gas = 0))
  }

  test("never more than seven alive") {
    KillCredits.reset()
    assert( ! DarkTemplarEffectiveness.allows(produced = 9, alive = 7, gas = 9999))
  }

  test("swimming in gas keeps building on a break-even book") {
    KillCredits.reset()
    assert(DarkTemplarEffectiveness.allows(produced = 4, alive = 4, gas = 400))
  }

  test("a scoreless book blocks the fifth on a thin bank") {
    KillCredits.reset()
    assert( ! DarkTemplarEffectiveness.allows(produced = 4, alive = 4, gas = 399))
  }
}
