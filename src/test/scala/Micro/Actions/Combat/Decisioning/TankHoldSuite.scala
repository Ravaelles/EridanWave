package Micro.Actions.Combat.Decisioning

import ProxyBwapi.UnitClasses.UnitClasses
import ProxyBwapi.UnitInfo.{ForeignUnitInfo, UnitInfo}
import bwapi.UnitType
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class TankHoldSuite extends AnyFunSuite {

  private def foe(target: UnitType, id: Int): UnitInfo = {
    TestKit.seedWorld()
    val bw = TestKit.mockUnit(target)
    val unit = new ForeignUnitInfo(bw, id)
    unit.changeUnitType(UnitClasses.get(target))
    unit
  }

  test("sieged and unsieged tanks both hold the line") {
    assert(TankHold.isTank(foe(UnitType.Terran_Siege_Tank_Siege_Mode, 1)))
    assert(TankHold.isTank(foe(UnitType.Terran_Siege_Tank_Tank_Mode, 2)))
  }

  test("only tanks pin the line, other units fight normally") {
    assert( ! TankHold.isTank(foe(UnitType.Terran_Marine, 3)))
    assert( ! TankHold.isTank(foe(UnitType.Terran_Vulture, 4)))
  }

  test("hold inside two tiles past our max range, walk otherwise") {
    assert(TankHold.shouldHold(targetIsTank = true, distBeyondRangeTiles = 0.0))
    assert(TankHold.shouldHold(targetIsTank = true, distBeyondRangeTiles = 2.0))
    assert( ! TankHold.shouldHold(targetIsTank = true, distBeyondRangeTiles = 2.01))
    assert( ! TankHold.shouldHold(targetIsTank = false, distBeyondRangeTiles = 0.0))
  }
}
