package Tactic.Squads.Decisions

import ClusterPosture.{Fight, HoldLine, Posture}
import org.scalatest.funsuite.AnyFunSuite

class VoteConfirmationSuite extends AnyFunSuite {

  private def stanceVote(confirmFrames: Int = 3) =
    new VoteConfirmation[Posture](HoldLine, Fight, confirmFrames)

  private def consensusVote(confirmFrames: Int = 3) =
    new VoteConfirmation[Boolean](fastPath = true, slowPath = false, confirmFrames)

  test("a fast-path proposal adopts instantly") {
    val filter = stanceVote()
    assert(filter.confirm(Fight, HoldLine) == HoldLine)
    assert(filter.confirm(HoldLine, HoldLine) == HoldLine)

    val consensus = consensusVote()
    var adopted = false
    adopted = consensus.confirm(adopted, proposed = true)
    assert(adopted)
    adopted = consensus.confirm(adopted, proposed = true)
    assert(adopted)
  }

  test("a slow-path proposal needs consecutive confirmation") {
    val filter = stanceVote()
    assert(filter.confirm(HoldLine, Fight) == HoldLine)
    assert(filter.confirm(HoldLine, Fight) == HoldLine)
    assert(filter.confirm(HoldLine, Fight) == Fight)

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
    var posture: Posture = HoldLine
    (1 to 20).foreach(_ => {
      posture = filter.confirm(posture, Fight)
      posture = filter.confirm(posture, HoldLine)
    })
    assert(posture == HoldLine)

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
    filter.confirm(HoldLine, Fight)
    filter.confirm(HoldLine, Fight)
    filter.confirm(HoldLine, HoldLine)
    assert(filter.confirm(HoldLine, Fight) == HoldLine)
    assert(filter.confirm(HoldLine, Fight) == HoldLine)
    assert(filter.confirm(HoldLine, Fight) == Fight)
  }

  test("an adopted slow-path value holds without re-confirming") {
    val filter = stanceVote()
    (1 to 3).foreach(_ => filter.confirm(HoldLine, Fight))
    assert(filter.confirm(Fight, Fight) == Fight)

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
