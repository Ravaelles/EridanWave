package Micro.Actions.Combat.Spells.Terran

import ProxyBwapi.Races.Terran
import org.scalatest.funsuite.AnyFunSuite

class CloakPolicySuite extends AnyFunSuite {

  test("each hull decloaks with its own tech") {
    assert(CloakPolicy.techFor(Terran.Ghost).contains(Terran.GhostCloak))
    assert(CloakPolicy.techFor(Terran.Wraith).contains(Terran.WraithCloak))
    assert(CloakPolicy.techFor(Terran.Marine).isEmpty)
  }
}
