package Tactic.Tactics.Scouting.Monitor

import org.scalatest.funsuite.AnyFunSuite

class MonitorEscortSuite extends AnyFunSuite {

  test("an army out against known Lurkers gets an escort") {
    assert(MonitorEscort.applies(armyOut = true, lurkersKnown = true))
  }

  test("no army out means no escort") {
    assert( ! MonitorEscort.applies(armyOut = false, lurkersKnown = true))
  }

  test("no known Lurkers means no escort") {
    assert( ! MonitorEscort.applies(armyOut = true, lurkersKnown = false))
  }
}
