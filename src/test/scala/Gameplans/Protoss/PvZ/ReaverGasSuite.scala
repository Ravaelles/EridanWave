package Gameplans.Protoss.PvZ

import org.scalatest.funsuite.AnyFunSuite

class ReaverGasSuite extends AnyFunSuite {

  test("the first Reaver always queues") {
    assert(ReaverGas.allowsReaver(reavers = 0, gas = 0))
  }

  test("the second Reaver needs two-fifty banked") {
    assert( ! ReaverGas.allowsReaver(reavers = 1, gas = 249))
    assert(ReaverGas.allowsReaver(reavers = 1, gas = 250))
  }

  test("the third and later need three-fifty banked") {
    assert( ! ReaverGas.allowsReaver(reavers = 2, gas = 349))
    assert(ReaverGas.allowsReaver(reavers = 2, gas = 350))
    assert(ReaverGas.allowsReaver(reavers = 5, gas = 400))
    assert( ! ReaverGas.allowsReaver(reavers = 5, gas = 0))
  }
}
