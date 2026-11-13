package Micro.Targeting.FiltersRequired

import org.scalatest.funsuite.AnyFunSuite

class TargetFilterHallCleanupSuite extends AnyFunSuite {

  test("a lone hall is the fight") {
    assert(TargetFilterHallCleanup.loneHallCleanup(targetIsTownHall = true, otherThreatNear = false))
    assert( ! TargetFilterHallCleanup.loneHallCleanup(targetIsTownHall = false, otherThreatNear = false))
  }

  test("any other reachable enemy vetoes the lone-hall cleanup") {
    assert( ! TargetFilterHallCleanup.loneHallCleanup(targetIsTownHall = true, otherThreatNear = true))
  }

  test("the lone-hall threat radius is eight tiles") {
    assert(TargetFilterHallCleanup.hallThreatTiles == 8.0)
  }
}
