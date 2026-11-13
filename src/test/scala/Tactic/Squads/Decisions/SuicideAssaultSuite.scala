package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class SuicideAssaultSuite extends AnyFunSuite {

  test("the charge holds inside thirty land tiles") {
    assert(SuicideAssault.charging(0.0))
    assert(SuicideAssault.charging(30.0))
    assert( ! SuicideAssault.charging(30.01))
    assert( ! SuicideAssault.charging(200.0))
  }

  test("the assault holds inside fifteen tiles of the enemy main") {
    assert(SuicideAssault.assaulting(0.0))
    assert(SuicideAssault.assaulting(15.0))
    assert( ! SuicideAssault.assaulting(15.01))
    assert( ! SuicideAssault.assaulting(200.0))
  }
}
