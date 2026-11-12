package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class ConsensusVoteFilterSuite extends AnyFunSuite {

  test("rejoining passes through instantly") {
    val filter = new ConsensusVoteFilter(confirmFrames = 24)
    var adopted = false
    adopted = filter.confirm(adopted, proposed = true)
    assert(adopted)
    adopted = filter.confirm(adopted, proposed = true)
    assert(adopted)
  }

  test("leaving needs a consecutive streak") {
    val filter = new ConsensusVoteFilter(confirmFrames = 3)
    var adopted = true
    adopted = filter.confirm(adopted, proposed = false)
    assert(adopted)
    adopted = filter.confirm(adopted, proposed = false)
    assert(adopted)
    adopted = filter.confirm(adopted, proposed = false)
    assert(!adopted)
  }

  test("alternating noise never accumulates a streak") {
    val filter = new ConsensusVoteFilter(confirmFrames = 3)
    var adopted = true
    adopted = filter.confirm(adopted, proposed = false)
    assert(adopted)
    adopted = filter.confirm(adopted, proposed = true)
    assert(adopted)
    adopted = filter.confirm(adopted, proposed = false)
    assert(adopted)
    adopted = filter.confirm(adopted, proposed = true)
    assert(adopted)
    adopted = filter.confirm(adopted, proposed = false)
    assert(adopted)
  }

  test("a held retreat stays retreating without re-confirming") {
    val filter = new ConsensusVoteFilter(confirmFrames = 2)
    var adopted = true
    adopted = filter.confirm(adopted, proposed = false)
    assert(adopted)
    adopted = filter.confirm(adopted, proposed = false)
    assert(!adopted)
    adopted = filter.confirm(adopted, proposed = false)
    assert(!adopted)
    adopted = filter.confirm(adopted, proposed = false)
    assert(!adopted)
  }
}
