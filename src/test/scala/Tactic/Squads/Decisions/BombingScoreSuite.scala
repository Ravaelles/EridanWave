package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class BombingScoreSuite extends AnyFunSuite {

  test("richer prey wins") {
    assert(BombingScore.score(200, 10, 1000) > BombingScore.score(100, 10, 1000))
  }

  test("anti-air cover discounts") {
    assert(BombingScore.score(200, 0, 1000) > BombingScore.score(200, 50, 1000))
  }

  test("nearer wins") {
    assert(BombingScore.score(200, 10, 500) > BombingScore.score(200, 10, 2000))
  }

  test("worthless prey scores zero") {
    assert(BombingScore.score(0, 0, 0) == 0)
  }
}
