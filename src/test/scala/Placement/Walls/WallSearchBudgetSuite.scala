package Placement.Walls

import org.scalatest.funsuite.AnyFunSuite

class WallSearchBudgetSuite extends AnyFunSuite {

  private def budget(
      maxPerConstraint  : Int = 256,
      maxPerPermutation : Int = 64,
      expired           : () => Boolean = () => false): WallSearchBudget =
    new WallSearchBudget(maxPerPermutation, maxPerConstraint, 0, 0, expired)

  test("a fresh budget keeps searching") {
    assert( ! budget().searchedEnough)
  }

  test("the constraint budget stops the search") {
    val b = budget(maxPerConstraint = 256)
    b.candidatesThisConstraint = 255
    assert( ! b.searchedEnough)
    b.candidatesThisConstraint = 256
    assert(b.searchedEnough)
  }

  test("the permutation budget stops the try") {
    val b = budget(maxPerPermutation = 64)
    b.candidatesThisPermutation = 63
    assert( ! b.searchedEnough)
    b.candidatesThisPermutation = 64
    assert(b.searchedEnough)
  }

  test("an expired search outranks the candidate budgets") {
    assert(budget(expired = () => true).searchedEnough)
  }
}
