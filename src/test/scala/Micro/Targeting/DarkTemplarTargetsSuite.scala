package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite
import ProxyBwapi.Races.{Protoss, Terran, Zerg}

class DarkTemplarTargetsSuite extends AnyFunSuite {

  private def mark(dist: Double, hp: Int, tank: Boolean = false, warrior: Boolean = true, unfinished: Boolean = false): DarkTemplarTargets.Mark =
    DarkTemplarTargets.Mark(dist, hp, tank, warrior, unfinished)

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

  test("unfinished statics outrank everything at any distance") {
    assert(DarkTemplarTargets.isUnfinishedStatic(isStaticDefense = true, complete = false))
    assert( ! DarkTemplarTargets.isUnfinishedStatic(isStaticDefense = true, complete = true))
    assert( ! DarkTemplarTargets.isUnfinishedStatic(isStaticDefense = false, complete = false))
    val cannon = mark(9.0, 300, tank = false, warrior = false, unfinished = true)
    val weak = mark(1.0, 20)
    val tank = mark(2.0, 200, tank = true)
    assert(DarkTemplarTargets.tier(cannon) < DarkTemplarTargets.tier(weak))
    assert(DarkTemplarTargets.pick(Seq(mark(1.0, 20, warrior = true), cannon)).contains(1))
  }

  test("pickUnfinished takes the nearest unfinished static, nothing when none") {
    val far = mark(9.0, 300, tank = false, warrior = false, unfinished = true)
    val near = mark(4.0, 300, tank = false, warrior = false, unfinished = true)
    assert(DarkTemplarTargets.pickUnfinished(Seq(mark(1.0, 20), far, near)).contains(2))
    assert(DarkTemplarTargets.pickUnfinished(Seq(mark(1.0, 20))).isEmpty)
    assert(DarkTemplarTargets.pickUnfinished(Seq.empty).isEmpty)
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
