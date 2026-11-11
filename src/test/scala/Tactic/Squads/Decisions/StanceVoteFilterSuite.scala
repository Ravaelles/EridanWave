package Tactic.Squads.Decisions

import RegroupLatch.{Engaged, Regrouping, Stance}
import org.scalatest.funsuite.AnyFunSuite

class StanceVoteFilterSuite extends AnyFunSuite {

  test("re-engage proposals adopt instantly") {
    val filter = new StanceVoteFilter(confirmFrames = 3)
    assert(filter.confirm(Regrouping, Engaged) == Engaged)
    assert(filter.confirm(Engaged, Engaged) == Engaged)
  }

  test("retreat needs consecutive confirmation") {
    val filter = new StanceVoteFilter(confirmFrames = 3)
    assert(filter.confirm(Engaged, Regrouping) == Engaged)
    assert(filter.confirm(Engaged, Regrouping) == Engaged)
    assert(filter.confirm(Engaged, Regrouping) == Regrouping)
  }

  test("alternating noise never accumulates a streak") {
    val filter = new StanceVoteFilter(confirmFrames = 3)
    var stance: Stance = Engaged
    (1 to 20).foreach(_ => {
      stance = filter.confirm(stance, Regrouping)
      stance = filter.confirm(stance, Engaged)
    })
    assert(stance == Engaged)
  }

  test("an interrupt resets the retreat streak") {
    val filter = new StanceVoteFilter(confirmFrames = 3)
    filter.confirm(Engaged, Regrouping)
    filter.confirm(Engaged, Regrouping)
    filter.confirm(Engaged, Engaged)
    assert(filter.confirm(Engaged, Regrouping) == Engaged)
    assert(filter.confirm(Engaged, Regrouping) == Engaged)
    assert(filter.confirm(Engaged, Regrouping) == Regrouping)
  }

  test("adopted regroup holds without re-confirmation") {
    val filter = new StanceVoteFilter(confirmFrames = 3)
    (1 to 3).foreach(_ => filter.confirm(Engaged, Regrouping))
    assert(filter.confirm(Regrouping, Regrouping) == Regrouping)
  }
}
