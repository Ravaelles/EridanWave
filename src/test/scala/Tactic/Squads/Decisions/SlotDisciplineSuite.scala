package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class SlotDisciplineSuite extends AnyFunSuite {

  private def slot(
    target: Boolean = false,
    ready: Boolean = false,
    sinceDamage: Int = 1000,
    threats: Boolean = false): Boolean =
    SlotDiscipline.maySlot(target, ready, sinceDamage, threats)

  test("an idle untouched unit may dress") {
    assert(slot())
  }

  test("a target, a ready weapon, a wound or a threat vetoes the slot") {
    assert( ! slot(target = true))
    assert( ! slot(ready = true))
    assert( ! slot(sinceDamage = 0))
    assert( ! slot(sinceDamage = 59))
    assert( ! slot(threats = true))
  }

  test("old damage stops vetoing after a minute of silence") {
    assert(slot(sinceDamage = 60))
  }
}
