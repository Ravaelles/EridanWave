package Micro.Actions.Protoss.DarkTemplar

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarBraverySuite extends AnyFunSuite {

  test("a duel the Templar wins outright is taken") {
    // Lone Templar (120hp, ~1.3dps) vs one full Dragoon (240hp, ~0.67dps).
    assert(DarkTemplarBravery.shouldDuel(ourHp = 120.0, ourDps = 1.3, foeHp = 240.0, foeDps = 0.0))
    assert(DarkTemplarBravery.shouldDuel(ourHp = 120.0, ourDps = 1.3, foeHp = 100.0, foeDps = 0.67))
  }

  test("a duel the pack wins is refused") {
    // Same Templar vs three full Dragoons answering: 156 vs 1600 power.
    assert( ! DarkTemplarBravery.shouldDuel(ourHp = 120.0, ourDps = 1.3, foeHp = 720.0, foeDps = 2.0))
    // Even two Dragoons outpower the lone hull.
    assert( ! DarkTemplarBravery.shouldDuel(ourHp = 120.0, ourDps = 1.3, foeHp = 480.0, foeDps = 1.33))
  }

  test("an even duel holds, backup turns it") {
    // Lone vs one: 156 vs 161 power - no edge, no duel.
    assert( ! DarkTemplarBravery.shouldDuel(ourHp = 120.0, ourDps = 1.3, foeHp = 240.0, foeDps = 0.67))
    // A Dragoon friend doubling our DPS: 312 vs 161 - duel.
    assert(DarkTemplarBravery.shouldDuel(ourHp = 120.0, ourDps = 2.6, foeHp = 240.0, foeDps = 0.67))
    // A wounded pack bleeds power: 156 vs 134 - duel.
    assert(DarkTemplarBravery.shouldDuel(ourHp = 120.0, ourDps = 1.3, foeHp = 200.0, foeDps = 0.67))
  }

  test("a Templar with no guns never duels") {
    assert( ! DarkTemplarBravery.shouldDuel(ourHp = 120.0, ourDps = 0.0, foeHp = 40.0, foeDps = 0.0))
  }
}
