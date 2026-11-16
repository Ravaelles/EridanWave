package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite
import ProxyBwapi.Races.{Protoss, Terran, Zerg}

class DarkTemplarTargetsSuite extends AnyFunSuite {

  private def mark(dist: Double, hp: Int, tank: Boolean = false, warrior: Boolean = true): DarkTemplarTargets.Mark =
    DarkTemplarTargets.Mark(dist, hp, tank, warrior)

  test("weak targets go first even when farther") {
    assert(DarkTemplarTargets.pick(Seq(mark(5.0, 100), mark(9.0, 39))).contains(1))
    assert(DarkTemplarTargets.pick(Seq(mark(5.0, 100), mark(9.0, 40))).contains(1))
    assert(DarkTemplarTargets.pick(Seq(mark(5.0, 100), mark(9.0, 41))).contains(0))
  }

  test("tanks outrank warriors outrank the rest") {
    assert(DarkTemplarTargets.pick(Seq(mark(1.0, 200, warrior = true), mark(9.0, 200, tank = true))).contains(1))
    assert(DarkTemplarTargets.pick(Seq(mark(1.0, 200, warrior = false), mark(5.0, 200, warrior = true))).contains(1))
  }

  test("nearest inside the tier wins, nothing means nothing") {
    assert(DarkTemplarTargets.pick(Seq(mark(9.0, 35), mark(2.0, 35))).contains(1))
    assert(DarkTemplarTargets.pick(Seq.empty).isEmpty)
  }

  test("prey is combat buildings, combat units and bases - never masonry or workers") {
    assert(DarkTemplarTargets.isPrey(Terran.SiegeTankSieged))
    assert(DarkTemplarTargets.isPrey(Terran.Marine))
    assert(DarkTemplarTargets.isPrey(Terran.MissileTurret))
    assert(DarkTemplarTargets.isPrey(Terran.Bunker))
    assert(DarkTemplarTargets.isPrey(Protoss.Nexus))
    assert(DarkTemplarTargets.isPrey(Protoss.Pylon) == false)
    assert(DarkTemplarTargets.isPrey(Terran.SupplyDepot) == false)
    assert(DarkTemplarTargets.isPrey(Protoss.Probe) == false)
    assert(DarkTemplarTargets.isPrey(Zerg.Drone) == false)
    assert(DarkTemplarTargets.isPrey(Terran.SCV) == false)
  }
}
