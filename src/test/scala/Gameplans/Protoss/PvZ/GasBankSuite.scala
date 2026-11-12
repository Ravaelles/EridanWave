package Gameplans.Protoss.PvZ.policy

import org.scalatest.funsuite.AnyFunSuite

class GasBankSuite extends AnyFunSuite {

  test("below ten Dragoons the bank never blocks a small army") {
    assert(GasBank.allowsDragoon(dragoons = 0, gas = 0, armyWarriors = 20))
    assert(GasBank.allowsDragoon(dragoons = 9, gas = 0, armyWarriors = 20))
  }

  test("past twenty warriors every Dragoon needs two-ten") {
    assert(GasBank.allowsDragoon(dragoons = 0, gas = 210, armyWarriors = 21))
    assert( ! GasBank.allowsDragoon(dragoons = 0, gas = 209, armyWarriors = 21))
    assert( ! GasBank.allowsDragoon(dragoons = 9, gas = 0, armyWarriors = 60))
    assert(GasBank.allowsDragoon(dragoons = 9, gas = 210, armyWarriors = 60))
  }

  test("from ten the bank holds two-fifty") {
    assert(GasBank.allowsDragoon(dragoons = 10, gas = 250, armyWarriors = 0))
    assert( ! GasBank.allowsDragoon(dragoons = 10, gas = 249, armyWarriors = 0))
    assert( ! GasBank.allowsDragoon(dragoons = 15, gas = 0, armyWarriors = 0))
  }

  test("past sixteen the bank holds three-sixty") {
    assert(GasBank.allowsDragoon(dragoons = 17, gas = 360, armyWarriors = 0))
    assert( ! GasBank.allowsDragoon(dragoons = 17, gas = 359, armyWarriors = 0))
    assert(GasBank.allowsDragoon(dragoons = 16, gas = 250, armyWarriors = 0))
  }
}
