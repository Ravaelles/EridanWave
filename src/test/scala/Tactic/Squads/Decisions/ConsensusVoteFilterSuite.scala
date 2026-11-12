package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class ConsensusVoteFilterSuite extends AnyFunSuite {

  test("rejoining passes through instantly") {
    val filter = new ConsensusVoteFilter(confirmFrames = 24)
    assert(filter.confirm(adopted = false, proposed = true))
    assert(filter.confirm(adopted = true, proposed = true))
  }

  test("leaving needs a consecutive streak") {
    val filter = new ConsensusVoteFilter(confirmFrames = 3)
    assert( ! filter.confirm(adopted = true, proposed = false))
    assert( ! filter.confirm(adopted = true, proposed = false))
    assert(filter.confirm(adopted = true, proposed = false))
  }

  test("alternating noise never accumulates a streak") {
    val filter = new ConsensusVoteFilter(confirmFrames = 3)
    assert( ! filter.confirm(adopted = true, proposed = false))
    assert(filter.confirm(adopted = false, proposed = true))
    assert( ! filter.confirm(adopted = true, proposed = false))
    assert(filter.confirm(adopted = false, proposed = true))
    assert( ! filter.confirm(adopted = true, proposed = false))
  }

  test("a held retreat stays retreating without re-confirming") {
    val filter = new ConsensusVoteFilter(confirmFrames = 2)
    assert( ! filter.confirm(adopted = true, proposed = false))
    assert(filter.confirm(adopted = true, proposed = false))
    assert( ! filter.confirm(adopted = false, proposed = false))
    assert( ! filter.confirm(adopted = false, proposed = false))
  }
}
