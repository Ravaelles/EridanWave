package Gameplans.Eridan.PvZ

import org.scalatest.funsuite.AnyFunSuite

class ZealotBankSuite extends AnyFunSuite {

  test("banked minerals plus two free Gateways queue more") {
    assert(ZealotBank.allowsExtraZealots(minerals = 560, freeGateways = 2))
    assert(ZealotBank.allowsExtraZealots(minerals = 900, freeGateways = 5))
  }

  test("a thin bank never queues more") {
    assert( ! ZealotBank.allowsExtraZealots(minerals = 559, freeGateways = 8))
  }

  test("busy Gateways never queue more") {
    assert( ! ZealotBank.allowsExtraZealots(minerals = 900, freeGateways = 1))
    assert( ! ZealotBank.allowsExtraZealots(minerals = 900, freeGateways = 0))
  }
}
