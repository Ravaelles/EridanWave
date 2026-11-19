package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class SlotDisciplineSuite extends AnyFunSuite {

  private def slot(
    firing: Boolean = false,
    sinceDamage: Int = 1000,
    verdict: Boolean = false,
    nearBuilding: Double = Double.PositiveInfinity,
    melee: Boolean = false,
    nearEnemy: Double = Double.PositiveInfinity): Boolean =
    SlotDiscipline.maySlot(firing, sinceDamage, verdict, nearBuilding, melee, nearEnemy)

  test("an idle untouched unit may dress") {
    assert(slot())
  }

  test("contact alone never vetoes the slot") {
    // A target on the ladder, a cooling weapon or a distant threat used
    // to veto; now only an actual shot does.
    assert(slot())
  }

  test("a firing gun, fresh wounds or a held retreat vetoes the slot") {
    assert( ! slot(firing = true))
    assert( ! slot(sinceDamage = 0))
    assert( ! slot(sinceDamage = 59))
    assert( ! slot(verdict = true))
  }

  test("old damage stops vetoing after a minute of silence") {
    assert(slot(sinceDamage = 60))
  }

  test("dressing stops inside eight tiles of an enemy building") {
    assert( ! slot(nearBuilding = 8.0))
    assert( ! slot(nearBuilding = 0.0))
    assert(slot(nearBuilding = 8.01))
  }

  test("a blade never dresses inside four tiles of an enemy") {
    assert( ! slot(melee = true, nearEnemy = 4.0))
    assert( ! slot(melee = true, nearEnemy = 0.0))
    assert(slot(melee = true, nearEnemy = 4.01))
    assert(slot(melee = false, nearEnemy = 0.0))
  }
}
