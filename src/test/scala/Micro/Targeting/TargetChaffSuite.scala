package Micro.Targeting

import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

class TargetChaffSuite extends AnyFunSuite {

  test("hatch chaff is never a target") {
    assert(TargetChaff.isChaff(Zerg.Larva))
    assert(TargetChaff.isChaff(Zerg.Egg))
  }

  test("everything else stays targetable") {
    assert( ! TargetChaff.isChaff(Zerg.Drone))
    assert( ! TargetChaff.isChaff(Zerg.Zergling))
    assert( ! TargetChaff.isChaff(Zerg.LurkerEgg))
    assert( ! TargetChaff.isChaff(Zerg.Cocoon))
    assert( ! TargetChaff.isChaff(Protoss.Dragoon))
  }
}
