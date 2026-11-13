package Tactic.Squads.Decisions

import RegroupLatch.{Engaged, Regrouping, Stance}
import org.scalatest.funsuite.AnyFunSuite

class VoteConfirmationSuite extends AnyFunSuite {

  private def stanceVote(confirmFrames: Int = 3) =
    new VoteConfirmation[Stance](Engaged, Regrouping, confirmFrames)

  private def consensusVote(confirmFrames: Int = 3) =
    new VoteConfirmation[Boolean](fastPath = true, slowPath = false, confirmFrames)

  test("a fast-path proposal adopts instantly") {
    val filter = stanceVote()
    assert(filter.confirm(Regrouping, Engaged) == Engaged)
    assert(filter.confirm(Engaged, Engaged) == Engaged)

    val consensus = consensusVote()
    var adopted = false
    adopted = consensus.confirm(adopted, proposed = true)
    assert(adopted)
    adopted = consensus.confirm(adopted, proposed = true)
    assert(adopted)
  }

  test("a slow-path proposal needs consecutive confirmation") {
    val filter = stanceVote()
    assert(filter.confirm(Engaged, Regrouping) == Engaged)
    assert(filter.confirm(Engaged, Regrouping) == Engaged)
    assert(filter.confirm(Engaged, Regrouping) == Regrouping)

    val consensus = consensusVote()
    var adopted = true
    adopted = consensus.confirm(adopted, proposed = false)
    assert(adopted)
    adopted = consensus.confirm(adopted, proposed = false)
    assert(adopted)
    adopted = consensus.confirm(adopted, proposed = false)
    assert(!adopted)
  }

  test("alternating noise never accumulates a streak") {
    val filter = stanceVote()
    var stance: Stance = Engaged
    (1 to 20).foreach(_ => {
      stance = filter.confirm(stance, Regrouping)
      stance = filter.confirm(stance, Engaged)
    })
    assert(stance == Engaged)

    val consensus = consensusVote()
    var adopted = true
    (1 to 10).foreach(_ => {
      adopted = consensus.confirm(adopted, proposed = false)
      adopted = consensus.confirm(adopted, proposed = true)
    })
    assert(adopted)
  }

  test("a fast-path interrupt resets the slow-path streak") {
    val filter = stanceVote()
    filter.confirm(Engaged, Regrouping)
    filter.confirm(Engaged, Regrouping)
    filter.confirm(Engaged, Engaged)
    assert(filter.confirm(Engaged, Regrouping) == Engaged)
    assert(filter.confirm(Engaged, Regrouping) == Engaged)
    assert(filter.confirm(Engaged, Regrouping) == Regrouping)
  }

  test("an adopted slow-path value holds without re-confirming") {
    val filter = stanceVote()
    (1 to 3).foreach(_ => filter.confirm(Engaged, Regrouping))
    assert(filter.confirm(Regrouping, Regrouping) == Regrouping)

    val consensus = consensusVote(confirmFrames = 2)
    var adopted = true
    (1 to 2).foreach(_ => adopted = consensus.confirm(adopted, proposed = false))
    assert(!adopted)
    adopted = consensus.confirm(adopted, proposed = false)
    assert(!adopted)
    adopted = consensus.confirm(adopted, proposed = false)
    assert(!adopted)
  }
}
