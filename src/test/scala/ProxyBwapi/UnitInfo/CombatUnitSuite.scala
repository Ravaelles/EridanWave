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

  test("shield wound fraction reads zero without shields, never NaN") {
    TestKit.seedWorld()
    // Zerg grows no shields: the zero-max guard must answer 0.0, not NaN.
    val zergling = TestKit.friendly(UnitType.Zerg_Zergling, id = 10)
    assert(zergling.shieldWoundPercent == 0.0)
  }

  test("shield and total wound bonuses scale linearly") {
    TestKit.seedWorld()
    // A fresh mock never ran update(), so its shields read full wound:
    // the bonus then sits exactly on the high end of the band.
    val dragoon = TestKit.friendly(UnitType.Protoss_Dragoon, id = 11)
    assert(dragoon.shieldWoundPercent == 1.0)
    assert(dragoon.shieldWoundBonus(0.0, 0.8) == 0.8)
    assert(dragoon.shieldWoundBonus(7.05, 7.85) == 7.85)
    // Fresh hull, full wound: the bonus band collapses onto the wound.
    assert(dragoon.woundBonus(0.0, 1.0) == dragoon.woundPercent())
  }
}
