package Macro.Actions

import org.scalatest.funsuite.AnyFunSuite

class ResearchBankedSuite extends AnyFunSuite {

  test("a thin bank researches nothing extra") {
    assert( ! ResearchBanked.banked(700, 501))
    assert( ! ResearchBanked.banked(701, 500))
    assert( ! ResearchBanked.banked(0, 0))
  }

  test("a deep bank opens research") {
    assert(ResearchBanked.banked(701, 501))
    assert(ResearchBanked.banked(5000, 5000))
  }
}
