package Gameplans.Protoss.PvP

import org.scalatest.funsuite.AnyFunSuite

class PvPWeakHoldSuite extends AnyFunSuite {

  test("a weak small army holds the natural") {
    assert(PvPWeakHold.holdNatural(armyStrengthUnits = 140.0, warriors = 9))
    assert(PvPWeakHold.holdNatural(armyStrengthUnits = 0.0, warriors = 0))
    assert( ! PvPWeakHold.holdNatural(armyStrengthUnits = 140.1, warriors = 0))
    assert( ! PvPWeakHold.holdNatural(armyStrengthUnits = 140.0, warriors = 10))
    assert( ! PvPWeakHold.holdNatural(armyStrengthUnits = 500.0, warriors = 30))
  }
}
