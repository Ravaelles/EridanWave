package Gameplans.Protoss.PvT

import org.scalatest.funsuite.AnyFunSuite

class ZealotSurgeSuite extends AnyFunSuite {

  test("banked minerals surge Zealots") {
    assert(ZealotSurge.allowsMassZealots(minerals = 581, supplyUsed = 100))
    assert(ZealotSurge.allowsMassZealots(minerals = 1400, supplyUsed = 184))
  }

  test("bank boundary sits past 580") {
    assert( ! ZealotSurge.allowsMassZealots(minerals = 580, supplyUsed = 100))
    assert(ZealotSurge.allowsMassZealots(minerals = 581, supplyUsed = 100))
  }

  test("surge stops at 185 supply") {
    assert( ! ZealotSurge.allowsMassZealots(minerals = 1400, supplyUsed = 185))
    assert( ! ZealotSurge.allowsMassZealots(minerals = 1400, supplyUsed = 200))
  }
}
