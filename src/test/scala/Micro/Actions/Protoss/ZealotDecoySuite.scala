package Micro.Actions.Protoss

import org.scalatest.funsuite.AnyFunSuite

class ZealotDecoySuite extends AnyFunSuite {

  test("a stripped Zealot with Dragoons nearby decoys melee") {
    assert(ZealotDecoy.shouldDecoy(shields = 16, rangedAlliesNear = 5, meleeFoesNear = 3))
    assert(ZealotDecoy.shouldDecoy(shields = 0, rangedAlliesNear = 1, meleeFoesNear = 1))
  }

  test("seventeen shields still duels") {
    assert( ! ZealotDecoy.shouldDecoy(shields = 17, rangedAlliesNear = 5, meleeFoesNear = 3))
  }

  test("no Dragoons or no melee means no decoy") {
    assert( ! ZealotDecoy.shouldDecoy(shields = 5, rangedAlliesNear = 0, meleeFoesNear = 3))
    assert( ! ZealotDecoy.shouldDecoy(shields = 5, rangedAlliesNear = 5, meleeFoesNear = 0))
    assert( ! ZealotDecoy.shouldDecoy(shields = 5, rangedAlliesNear = 0, meleeFoesNear = 0))
  }
}
