package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class StrongMarchSuite extends AnyFunSuite {

  test("a banked stock marches straight at the goal") {
    assert(StrongMarch.shouldForceAttack(minerals = 1000, armyStrength = 0.0, supplyUsed = 0))
    assert(StrongMarch.shouldForceAttack(minerals = 4000, armyStrength = 0.0, supplyUsed = 0))
    assert( ! StrongMarch.shouldForceAttack(minerals = 999, armyStrength = 0.0, supplyUsed = 0))
  }

  test("a maxed strong army marches straight, half-built still forms up") {
    assert(StrongMarch.shouldForceAttack(minerals = 0, armyStrength = 200.0, supplyUsed = 198))
    assert(StrongMarch.shouldForceAttack(minerals = 0, armyStrength = 400.0, supplyUsed = 200))
    assert( ! StrongMarch.shouldForceAttack(minerals = 0, armyStrength = 199.9, supplyUsed = 200))
    assert( ! StrongMarch.shouldForceAttack(minerals = 0, armyStrength = 400.0, supplyUsed = 197))
  }
}
