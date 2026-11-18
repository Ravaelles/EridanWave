package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class KeepCurrentSuite extends AnyFunSuite {

  test("a challenger inside the margin does not steal the pick") {
    assert(KeepCurrent.keepsTarget(currentScore = 10.0, bestChallengerScore = 11.0))
  }

  test("equality keeps the current pick") {
    assert(KeepCurrent.keepsTarget(currentScore = 10.0, bestChallengerScore = 12.0))
  }

  test("a challenger past the margin takes it the same frame") {
    assert( ! KeepCurrent.keepsTarget(currentScore = 10.0, bestChallengerScore = 12.1))
  }

  test("doctrine gaps always outrank stickiness") {
    assert( ! KeepCurrent.keepsTarget(currentScore = 10.0, bestChallengerScore = 22.0))
  }
}
