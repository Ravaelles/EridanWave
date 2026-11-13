package Micro.Actions.Combat.Spells.Protoss

import org.scalatest.funsuite.AnyFunSuite

class PsionicStormSuite extends AnyFunSuite {

  test("two own units may share a storm") {
    assert( ! PsionicStorm.vetoFriendlyHits(0))
    assert( ! PsionicStorm.vetoFriendlyHits(2))
  }

  test("three own units veto the storm") {
    assert(PsionicStorm.vetoFriendlyHits(3))
    assert(PsionicStorm.vetoFriendlyHits(12))
  }

  test("Overlords never eat storms off a half battery") {
    assert(PsionicStorm.vetoOverlord(isOverlord = true, energy = 0))
    assert(PsionicStorm.vetoOverlord(isOverlord = true, energy = 190))
    assert( ! PsionicStorm.vetoOverlord(isOverlord = true, energy = 191))
    assert( ! PsionicStorm.vetoOverlord(isOverlord = false, energy = 0))
  }
}
