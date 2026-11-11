package Micro.Actions.Protoss

import org.scalatest.funsuite.AnyFunSuite

class ZealotDecoySuite extends AnyFunSuite {

  test("a stripped Zealot with Dragoons nearby decoys melee") {
    assert(ZealotDecoy.shouldDecoy(shields = 16, rangedAlliesNear = 5, meleeFoesNear = 3, combatUnits = 10))
    assert(ZealotDecoy.shouldDecoy(shields = 0, rangedAlliesNear = 1, meleeFoesNear = 1, combatUnits = 24))
  }

  test("seventeen shields still duels") {
    assert( ! ZealotDecoy.shouldDecoy(shields = 17, rangedAlliesNear = 5, meleeFoesNear = 3, combatUnits = 24))
  }

  test("no Dragoons or no melee means no decoy") {
    assert( ! ZealotDecoy.shouldDecoy(shields = 5, rangedAlliesNear = 0, meleeFoesNear = 3, combatUnits = 24))
    assert( ! ZealotDecoy.shouldDecoy(shields = 5, rangedAlliesNear = 5, meleeFoesNear = 0, combatUnits = 24))
    assert( ! ZealotDecoy.shouldDecoy(shields = 5, rangedAlliesNear = 0, meleeFoesNear = 0, combatUnits = 24))
  }

  test("small armies never decoy") {
    assert( ! ZealotDecoy.shouldDecoy(shields = 0, rangedAlliesNear = 5, meleeFoesNear = 3, combatUnits = 9))
    assert(ZealotDecoy.shouldDecoy(shields = 0, rangedAlliesNear = 5, meleeFoesNear = 3, combatUnits = 10))
  }
}
