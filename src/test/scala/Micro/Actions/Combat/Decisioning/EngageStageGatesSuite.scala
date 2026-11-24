package Micro.Actions.Combat.Decisioning

import bwapi.UnitType
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

/**
 * Engage-pipeline gate tests: every stage states up front which units
 * it ever applies to, before any map, target or verdict is read. The
 * run keeps its own checks for direct callers; the stage gate is the
 * reviewed contract the dispatch obeys.
 */
class EngageStageGatesSuite extends AnyFunSuite {

  test("contain fire is ungated by unit: the vote and the target gate the shot") {
    TestKit.seedWorld()
    assert(EngagePipeline.ContainFireStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert(EngagePipeline.ContainFireStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Probe, 2)))
  }

  test("the advance gate is the march's own gait: every hull") {
    TestKit.seedWorld()
    assert(EngagePipeline.AdvanceGateStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert(EngagePipeline.AdvanceGateStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Probe, 2)))
  }

  test("only blades answer the leash") {
    TestKit.seedWorld()
    assert(EngagePipeline.ZealotLeashStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 1)))
    assert( ! EngagePipeline.ZealotLeashStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 2)))
  }

  test("only warriors rejoin the cluster") {
    TestKit.seedWorld()
    assert(EngagePipeline.ClusterRejoinStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 1)))
    assert( ! EngagePipeline.ClusterRejoinStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Probe, 2)))
  }
}
