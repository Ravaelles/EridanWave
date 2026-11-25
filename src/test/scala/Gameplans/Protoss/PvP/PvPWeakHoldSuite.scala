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

  test("pressure at the gates holds the natural even before weakness does") {
    assert(PvPWeakHold.pressuredTiles == 60.0)
    assert(PvPWeakHold.pressuredArmyUnits == 190.0)
    assert(PvPWeakHold.pressuredWarriors == 10)
    // Enemy on top of our main with a thin line: no second base.
    assert(PvPWeakHold.holdNaturalVsPressure(armyStrengthUnits = 190.0, warriors = 10, nearestEnemyTiles = 60.0))
    assert(PvPWeakHold.holdNaturalVsPressure(armyStrengthUnits = 100.0, warriors = 5, nearestEnemyTiles = 0.0))
    // Strong line, full count, or distant enemy: expand freely.
    assert( ! PvPWeakHold.holdNaturalVsPressure(armyStrengthUnits = 190.1, warriors = 10, nearestEnemyTiles = 60.0))
    assert( ! PvPWeakHold.holdNaturalVsPressure(armyStrengthUnits = 190.0, warriors = 11, nearestEnemyTiles = 60.0))
    assert( ! PvPWeakHold.holdNaturalVsPressure(armyStrengthUnits = 190.0, warriors = 10, nearestEnemyTiles = 60.1))
  }

  test("a weak small army holds any expansion, not just an unpressured natural") {
    assert(PvPWeakHold.weakExpandArmyUnits == 120.0)
    assert(PvPWeakHold.weakExpandWarriors == 30)
    // Thin weak line: no second base.
    assert(PvPWeakHold.holdExpansion(armyStrengthUnits = 120.0, warriors = 29))
    assert(PvPWeakHold.holdExpansion(armyStrengthUnits = 0.0, warriors = 0))
    // Strong line or full count: expand freely.
    assert( ! PvPWeakHold.holdExpansion(armyStrengthUnits = 120.1, warriors = 0))
    assert( ! PvPWeakHold.holdExpansion(armyStrengthUnits = 120.0, warriors = 30))
    assert( ! PvPWeakHold.holdExpansion(armyStrengthUnits = 500.0, warriors = 40))
  }
}
