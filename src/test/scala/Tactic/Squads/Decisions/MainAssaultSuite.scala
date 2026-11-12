package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class MainAssaultSuite extends AnyFunSuite {

  test("the assault holds inside fifteen tiles of the enemy main") {
    assert(MainAssault.assaulting(0.0))
    assert(MainAssault.assaulting(15.0))
    assert( ! MainAssault.assaulting(15.01))
    assert( ! MainAssault.assaulting(200.0))
  }
}
