package Tactic.Squads

import org.scalatest.funsuite.AnyFunSuite

class MarchOrdersSuite extends AnyFunSuite {

  test("a dueling unit holds instead of marching the legs") {
    assert(MarchOrders.duelHoldsMarch(verdictRetreat = false, hasAttackTarget = true, engagedUpon = false))
    assert(MarchOrders.duelHoldsMarch(verdictRetreat = false, hasAttackTarget = false, engagedUpon = true))
    assert(MarchOrders.duelHoldsMarch(verdictRetreat = false, hasAttackTarget = true, engagedUpon = true))
  }

  test("a retreating unit marches home, a calm one marches out") {
    assert( ! MarchOrders.duelHoldsMarch(verdictRetreat = true, hasAttackTarget = true, engagedUpon = true))
    assert( ! MarchOrders.duelHoldsMarch(verdictRetreat = false, hasAttackTarget = false, engagedUpon = false))
  }
}
