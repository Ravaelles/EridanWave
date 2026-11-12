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
}
