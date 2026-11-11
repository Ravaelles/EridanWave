package Tactic

import org.scalatest.funsuite.AnyFunSuite

class ClusterSplitSuite extends AnyFunSuite {

  test("forty-five percent may leave, fifty-five stays") {
    assert(ClusterSplit.maxDelegated(100) == 45)
    assert(ClusterSplit.maxDelegated(20) == 9)
    assert(ClusterSplit.maxDelegated(0) == 0)
  }

  test("over-delegation trips past the cap") {
    assert(ClusterSplit.overDelegated(totalWarriors = 100, delegated = 46))
    assert( ! ClusterSplit.overDelegated(totalWarriors = 100, delegated = 45))
    assert( ! ClusterSplit.overDelegated(totalWarriors = 10, delegated = 4))
  }
}
