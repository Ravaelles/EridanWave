package Gameplans.Protoss.PvZ

import org.scalatest.funsuite.AnyFunSuite

class GasBankSuite extends AnyFunSuite {

  test("below ten Dragoons the bank never blocks") {
    assert(GasBank.allowsDragoon(dragoons = 0, gas = 0))
    assert(GasBank.allowsDragoon(dragoons = 9, gas = 0))
  }

  test("from ten the bank holds two-fifty") {
    assert(GasBank.allowsDragoon(dragoons = 10, gas = 250))
    assert( ! GasBank.allowsDragoon(dragoons = 10, gas = 249))
    assert( ! GasBank.allowsDragoon(dragoons = 15, gas = 0))
  }

  test("past sixteen the bank holds three-sixty") {
    assert(GasBank.allowsDragoon(dragoons = 17, gas = 360))
    assert( ! GasBank.allowsDragoon(dragoons = 17, gas = 359))
    assert(GasBank.allowsDragoon(dragoons = 16, gas = 250))
  }
}
