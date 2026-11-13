package Tactic.Missions.drops

import org.scalatest.funsuite.AnyFunSuite

class FerryIdleDraftSuite extends AnyFunSuite {

  test("healthy taskless shuttles get drafted") {
    assert(FerryIdleDraft.shouldDraft(healthFraction = 1.0, hasTask = false, hasMoveOrder = false))
    assert(FerryIdleDraft.shouldDraft(healthFraction = 0.6, hasTask = true, hasMoveOrder = false))
    assert(FerryIdleDraft.shouldDraft(healthFraction = 0.6, hasTask = false, hasMoveOrder = true))
  }

  test("bleeding or busy shuttles stay home") {
    assert( ! FerryIdleDraft.shouldDraft(healthFraction = 0.59, hasTask = false, hasMoveOrder = false))
    assert( ! FerryIdleDraft.shouldDraft(healthFraction = 1.0, hasTask = true, hasMoveOrder = true))
    assert( ! FerryIdleDraft.shouldDraft(healthFraction = 0.0, hasTask = false, hasMoveOrder = false))
  }
}
