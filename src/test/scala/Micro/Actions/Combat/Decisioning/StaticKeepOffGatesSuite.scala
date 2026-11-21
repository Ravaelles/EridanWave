package Micro.Actions.Combat.Decisioning

import bwapi.UnitType
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class StaticKeepOffGatesSuite extends AnyFunSuite {

  test("shells turn mobile grounded hulls, never Reavers") {
    TestKit.seedWorld()
    assert(HandleEnemyCombatBuildings.ShellsClaim.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! HandleEnemyCombatBuildings.ShellsClaim.appliesToUnit(TestKit.friendly(UnitType.Protoss_Reaver, 2)))
    assert( ! HandleEnemyCombatBuildings.ShellsClaim.appliesToUnit(TestKit.friendly(UnitType.Protoss_Observer, 3)))
  }

  test("the keep-off line is drawn for scouts") {
    TestKit.seedWorld()
    assert(HandleEnemyCombatBuildings.KeepOffClaim.appliesToUnit(TestKit.friendly(UnitType.Protoss_Scout, 1)))
    assert( ! HandleEnemyCombatBuildings.KeepOffClaim.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 2)))
  }

  test("the contain-stand is a ranged ground warrior's post") {
    TestKit.seedWorld()
    assert(HandleEnemyCombatBuildings.StandClaim.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! HandleEnemyCombatBuildings.StandClaim.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 2)))
    assert( ! HandleEnemyCombatBuildings.StandClaim.appliesToUnit(TestKit.friendly(UnitType.Protoss_Corsair, 3)))
  }
}
