package ProxyBwapi.UnitInfo

import bwapi.UnitType
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class CombatUnitSuite extends AnyFunSuite {

  test("mocked units resolve their static class after update") {
    TestKit.seedWorld()
    val dragoon = TestKit.friendly(UnitType.Protoss_Dragoon, id = 1)
    assert(dragoon.unitClass == ProxyBwapi.Races.Protoss.Dragoon)
    assert(dragoon.pixelRangeMin == ProxyBwapi.Races.Protoss.Dragoon.groundMinRangeRaw)
  }

  test("melee range reads zero") {
    TestKit.seedWorld()
    val zealot = TestKit.friendly(UnitType.Protoss_Zealot, id = 2)
    assert(zealot.pixelRangeMin == 0)
  }

  test("unclassified units match nothing instead of throwing") {
    TestKit.seedWorld()
    val unit = new FriendlyUnitInfo(TestKit.mockUnit(UnitType.Protoss_Zealot), 3)
    assert(unit.unitClass == ProxyBwapi.UnitClasses.UnitClasses.None)
    assert( ! ProxyBwapi.Races.Protoss.Zealot(unit))
    assert( ! unit.unitClass.isBuilding)
  }
}
