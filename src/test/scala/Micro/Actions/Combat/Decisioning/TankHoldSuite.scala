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

  test("a dragoon stands off a sieged tank it cannot outrange") {
    // Sieged reach 12: hold the 12-14 standoff, never inside the
    // footprint (the withdrawal owns that) and never past it (the
    // approach owns that).
    assert(TankHold.shouldHold(targetIsTank = true, distTiles = 12.0, tankReachTiles = 12.0))
    assert(TankHold.shouldHold(targetIsTank = true, distTiles = 13.0, tankReachTiles = 12.0))
    assert(TankHold.shouldHold(targetIsTank = true, distTiles = 14.0, tankReachTiles = 12.0))
    assert( ! TankHold.shouldHold(targetIsTank = true, distTiles = 11.9, tankReachTiles = 12.0))
    assert( ! TankHold.shouldHold(targetIsTank = true, distTiles = 6.0, tankReachTiles = 12.0))
    assert( ! TankHold.shouldHold(targetIsTank = true, distTiles = 14.01, tankReachTiles = 12.0))
  }

  test("an unsieged tank holds its own shorter standoff") {
    assert(TankHold.shouldHold(targetIsTank = true, distTiles = 8.0, tankReachTiles = 7.0))
    assert( ! TankHold.shouldHold(targetIsTank = true, distTiles = 6.0, tankReachTiles = 7.0))
    assert( ! TankHold.shouldHold(targetIsTank = true, distTiles = 9.01, tankReachTiles = 7.0))
  }

  test("sieged and unsieged tanks both pin the line") {
    assert(TankHold.isTank(foe(UnitType.Terran_Siege_Tank_Siege_Mode, 1)))
    assert(TankHold.isTank(foe(UnitType.Terran_Siege_Tank_Tank_Mode, 2)))
  }

  test("only tanks pin the line, other units fight normally") {
    assert( ! TankHold.isTank(foe(UnitType.Terran_Marine, 3)))
    assert( ! TankHold.isTank(foe(UnitType.Terran_Vulture, 4)))
  }

  test("a parked hull inside the footprint never holds") {
    assert( ! TankHold.shouldHold(targetIsTank = true, distTiles = 5.0, tankReachTiles = 12.0))
    assert( ! TankHold.shouldHold(targetIsTank = false, distTiles = 13.0, tankReachTiles = 12.0))
  }
}
