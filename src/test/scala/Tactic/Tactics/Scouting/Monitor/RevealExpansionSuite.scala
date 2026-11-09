package Tactic.Tactics.Scouting.Monitor

import org.scalatest.funsuite.AnyFunSuite

class RevealExpansionSuite extends AnyFunSuite {

  test("a Lurker inside 50 tiles of main pins the first eye") {
    assert(RevealExpansionBlockedWithHiddenUnit.lurkerThreatensMain(50 * 32 - 1))
    assert( ! RevealExpansionBlockedWithHiddenUnit.lurkerThreatensMain(50 * 32))
  }

  test("presence inside 4 tiles blocks the site") {
    assert(RevealExpansionBlockedWithHiddenUnit.siteBlocked(4 * 32 - 1))
    assert( ! RevealExpansionBlockedWithHiddenUnit.siteBlocked(4 * 32))
  }
}
