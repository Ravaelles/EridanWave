package Gameplans.Protoss.PvE

import org.scalatest.funsuite.AnyFunSuite

class ProxyGasTimingSuite extends AnyFunSuite {

  test("a standing line gases up") {
    assert(ProxyGasTiming.shouldGas(zealots = 9, zealotsTotal = 9, hydraShown = false, pastDeadline = false))
    assert(ProxyGasTiming.shouldGas(zealots = 20, zealotsTotal = 20, hydraShown = false, pastDeadline = false))
  }

  test("a bled-out proxy gases up on total") {
    assert(ProxyGasTiming.shouldGas(zealots = 5, zealotsTotal = 13, hydraShown = false, pastDeadline = false))
    assert( ! ProxyGasTiming.shouldGas(zealots = 8, zealotsTotal = 12, hydraShown = false, pastDeadline = false))
  }

  test("hydralisks or the clock force gas") {
    assert(ProxyGasTiming.shouldGas(zealots = 2, zealotsTotal = 2, hydraShown = true, pastDeadline = false))
    assert(ProxyGasTiming.shouldGas(zealots = 2, zealotsTotal = 2, hydraShown = false, pastDeadline = true))
  }
}
