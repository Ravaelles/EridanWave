package Information.Battles

import org.scalatest.funsuite.AnyFunSuite

class ArmyStrengthSuite extends AnyFunSuite {

  test("even strength scores a hundred") {
    assert(ArmyStrength.score(100.0, 100.0) == 100.0)
    assert(ArmyStrength.score(0.0, 0.0) == 100.0)
  }

  test("no scouted enemy counts as even") {
    assert(ArmyStrength.score(50.0, 0.0) == 100.0)
    assert(ArmyStrength.score(50.0, -10.0) == 100.0)
  }

  test("double and half scale linearly") {
    assert(ArmyStrength.score(200.0, 100.0) == 200.0)
    assert(ArmyStrength.score(50.0, 100.0) == 50.0)
  }

  test("below one-thirty the attack squad breaks off") {
    assert(ArmyStrength.retreating(ours = 124.0, foe = 100.0))
    assert(ArmyStrength.retreating(ours = 50.0, foe = 100.0))
    assert( ! ArmyStrength.retreating(ours = 130.0, foe = 100.0))
    assert( ! ArmyStrength.retreating(ours = 200.0, foe = 100.0))
    assert( ! ArmyStrength.retreating(ours = 50.0, foe = 0.0))
  }

  test("a known Zerg expo dies to the main force at one-forty mobile") {
    assert(ArmyStrength.shouldPunishExpo(isZerg = true, expoKnown = true, oursUnits = 140.0, foeUnits = 100.0))
    assert(ArmyStrength.shouldPunishExpo(isZerg = true, expoKnown = true, oursUnits = 280.0, foeUnits = 100.0))
    assert( ! ArmyStrength.shouldPunishExpo(isZerg = true, expoKnown = true, oursUnits = 139.0, foeUnits = 100.0))
    assert( ! ArmyStrength.shouldPunishExpo(isZerg = true, expoKnown = false, oursUnits = 500.0, foeUnits = 100.0))
    assert( ! ArmyStrength.shouldPunishExpo(isZerg = false, expoKnown = true, oursUnits = 500.0, foeUnits = 100.0))
  }

  test("unit counts score like skim, with a one-forty floor of their own") {
    assert(ArmyStrength.units(100.0, 100.0) == 100.0)
    assert(ArmyStrength.units(70.0, 50.0) == 140.0)
    assert(ArmyStrength.retreatBelowUnits == 140.0)
  }

  test("UMS doubles the fight margin: the unit floor halves, retreats fire at half the odds") {
    assert(ArmyStrength.retreatBelowUnitsFor(umsMode = false) == 140.0)
    assert(ArmyStrength.retreatBelowUnitsFor(umsMode = true) == 70.0)
  }
}
