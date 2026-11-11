package Tactic.Tactics.Scouting.Monitor

import org.scalatest.funsuite.AnyFunSuite

class RevealExpansionSuite extends AnyFunSuite {

  test("a Lurker inside 50 tiles of main pins the first eye") {
    assert(RevealExpansionBlockedWithHiddenUnit.lurkerThreatensMain(50 - 1.0 / 32))
    assert( ! RevealExpansionBlockedWithHiddenUnit.lurkerThreatensMain(50))
  }

  test("presence inside 4 tiles blocks the site") {
    assert(RevealExpansionBlockedWithHiddenUnit.siteBlocked(4 - 1.0 / 32))
    assert( ! RevealExpansionBlockedWithHiddenUnit.siteBlocked(4))
  }
}
