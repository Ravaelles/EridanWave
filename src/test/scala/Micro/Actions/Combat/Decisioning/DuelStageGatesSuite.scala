package Micro.Actions.Combat.Decisioning

import bwapi.UnitType
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class DuelStageGatesSuite extends AnyFunSuite {

  test("pushing is for grounded hulls: fliers dance instead") {
    TestKit.seedWorld()
    assert(EngagePipeline.PushDuelStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! EngagePipeline.PushDuelStage.appliesToUnit(TestKit.friendly(UnitType.Zerg_Mutalisk, 2)))
  }

  test("scavenging is technique-owned: any duelist may pick at the edge") {
    TestKit.seedWorld()
    assert(EngagePipeline.ScavengeDuelStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert(EngagePipeline.ScavengeDuelStage.appliesToUnit(TestKit.friendly(UnitType.Zerg_Zergling, 2)))
  }

  test("forming is open to every warrior: the behavior gates the how") {
    TestKit.seedWorld()
    assert(EngagePipeline.FormationDuelStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert(EngagePipeline.FormationDuelStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 2)))
  }

  test("closing is owned by the trio below, not by unit kind") {
    TestKit.seedWorld()
    assert(EngagePipeline.CloseDuelStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert(EngagePipeline.CloseDuelStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Probe, 2)))
  }

  test("the terminal repositions whoever the duel leaves behind") {
    TestKit.seedWorld()
    assert(EngagePipeline.RepositionDuelStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert(EngagePipeline.RepositionDuelStage.appliesToUnit(TestKit.friendly(UnitType.Zerg_Overlord, 2)))
  }
}
