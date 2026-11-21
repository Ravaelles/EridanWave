package Tactic.Squads

import org.scalatest.funsuite.AnyFunSuite

class MarchOrdersSuite extends AnyFunSuite {

  test("a latched duel holds instead of marching the legs") {
    assert(MarchOrders.duelHoldsMarch(verdictRetreat = false, verdictDuel = true))
    assert( ! MarchOrders.duelHoldsMarch(verdictRetreat = true, verdictDuel = true))
    assert( ! MarchOrders.duelHoldsMarch(verdictRetreat = false, verdictDuel = false))
  }
}
