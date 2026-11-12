package ProxyBwapi.UnitInfo

import bwapi.UnitType
import org.mockito.Mockito
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

  // The helper only reads woundPercent: stub it on a trait mock so the
  // tests pin the lerp without dragging the proxy/grid world along.
  // (FriendlyUnitInfo itself is final and unmockable.)
  private def bonusedUnit(wound: Double): CombatUnit = {
    val unit = Mockito.mock(classOf[CombatUnit], Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS))
    Mockito.when(unit.woundPercent()).thenReturn(wound)
    unit
  }

  test("a fresh hull takes the low edge of the wound bonus") {
    assert(bonusedUnit(0.0).woundPercentBonus(2.5, 6.5) == 2.5)
  }

  test("a dying hull takes the high edge of the wound bonus") {
    assert(bonusedUnit(1.0).woundPercentBonus(2.5, 6.5) == 6.5)
  }

  test("the wound bonus interpolates linearly and clamps") {
    assert(bonusedUnit(0.5).woundPercentBonus(0.0, 8.0) == 4.0)
    assert(bonusedUnit(-1.0).woundPercentBonus(0.0, 8.0) == 0.0)
    assert(bonusedUnit(2.0).woundPercentBonus(0.0, 8.0) == 8.0)
  }
}
