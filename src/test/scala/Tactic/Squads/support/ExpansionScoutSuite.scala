package Tactic.Squads.support

import org.scalatest.funsuite.AnyFunSuite

class ExpansionScoutSuite extends AnyFunSuite {

  test("a Dragoon scouts the expansions from eighty supply") {
    assert(ExpansionScout.wantsDragoonScout(80))
    assert(ExpansionScout.wantsDragoonScout(200))
    assert( ! ExpansionScout.wantsDragoonScout(79))
    assert( ! ExpansionScout.wantsDragoonScout(0))
  }

  test("a scouted expansion is revisited at least every minute") {
    assert(ExpansionScout.revisitFrames == 1440)
  }
}
