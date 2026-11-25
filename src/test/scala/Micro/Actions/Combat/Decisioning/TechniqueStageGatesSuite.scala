package Micro.Actions.Combat.Decisioning

import bwapi.UnitType
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class TechniqueStageGatesSuite extends AnyFunSuite {

  test("live storms turn any mobile hull, never buildings") {
    TestKit.seedWorld()
    assert(TechniquePipeline.AvoidEffectsStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert(TechniquePipeline.AvoidEffectsStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Probe, 2)))
    assert( ! TechniquePipeline.AvoidEffectsStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Pylon, 3)))
  }

  test("unseen killers turn grounded warriors, never the sky") {
    TestKit.seedWorld()
    assert(TechniquePipeline.AvoidStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! TechniquePipeline.AvoidStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Probe, 2)))
    assert( ! TechniquePipeline.AvoidStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Corsair, 3)))
  }

  test("shells turn mobile ground hulls, never Reavers") {
    TestKit.seedWorld()
    assert(TechniquePipeline.StaticKeepOffStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! TechniquePipeline.StaticKeepOffStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Reaver, 2)))
    assert( ! TechniquePipeline.StaticKeepOffStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Observer, 3)))
  }

  test("walking needs legs, aiming needs their absence") {
    TestKit.seedWorld()
    assert(TechniquePipeline.WalkStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! TechniquePipeline.WalkStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Pylon, 2)))
    assert(TechniquePipeline.AimStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Pylon, 3)))
    assert( ! TechniquePipeline.AimStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 4)))
  }

  test("dodging needs a mobile unboarded hull") {
    TestKit.seedWorld()
    assert(TechniquePipeline.DodgeStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! TechniquePipeline.DodgeStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Pylon, 2)))
  }

  test("potshots claim the cadence for any hull the technique walks out") {
    TestKit.seedWorld()
    assert(TechniquePipeline.PotshotStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert(TechniquePipeline.PotshotStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Probe, 2)))
  }

  test("always-attack answers for grounded warriors") {
    TestKit.seedWorld()
    assert(TechniquePipeline.AlwaysAttackStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! TechniquePipeline.AlwaysAttackStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Probe, 2)))
    assert( ! TechniquePipeline.AlwaysAttackStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Corsair, 3)))
  }

  test("the templar hunt is a grounded warrior's job, never the prey's own") {
    TestKit.seedWorld()
    assert(TechniquePipeline.DarkTemplarHuntStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! TechniquePipeline.DarkTemplarHuntStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Reaver, 2)))
    assert( ! TechniquePipeline.DarkTemplarHuntStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dark_Templar, 3)))
    assert( ! TechniquePipeline.DarkTemplarHuntStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Corsair, 4)))
  }

  test("dragoon doctrines answer dragoons") {
    TestKit.seedWorld()
    assert(TechniquePipeline.DragoonStandStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! TechniquePipeline.DragoonStandStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 2)))
    assert(TechniquePipeline.DragoonCarrierStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 3)))
    assert( ! TechniquePipeline.DragoonCarrierStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 4)))
    assert(TechniquePipeline.DragoonMineDefuseStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 5)))
    assert( ! TechniquePipeline.DragoonMineDefuseStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 6)))
  }

  test("the rearguard is squad-owned: any hull may hold the line") {
    TestKit.seedWorld()
    assert(TechniquePipeline.RearguardStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert(TechniquePipeline.RearguardStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Probe, 2)))
  }

  test("retreats walk out on legs") {
    TestKit.seedWorld()
    assert(TechniquePipeline.RetreatStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! TechniquePipeline.RetreatStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Pylon, 2)))
  }

  test("brawls are for mobile melee, never for guns") {
    TestKit.seedWorld()
    assert(TechniquePipeline.BrawlStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 1)))
    assert(TechniquePipeline.BrawlStage.appliesToUnit(TestKit.friendly(UnitType.Zerg_Zergling, 2)))
    assert( ! TechniquePipeline.BrawlStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 3)))
  }

  test("abuse is a whitelist: listed kiters only") {
    TestKit.seedWorld()
    assert(TechniquePipeline.AbuseStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! TechniquePipeline.AbuseStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 2)))
  }

  test("superkite spins up for Wraiths and Mutalisks only") {
    TestKit.seedWorld()
    assert(TechniquePipeline.SuperKiteStage.appliesToUnit(TestKit.friendly(UnitType.Terran_Wraith, 1)))
    assert(TechniquePipeline.SuperKiteStage.appliesToUnit(TestKit.friendly(UnitType.Zerg_Mutalisk, 2)))
    assert( ! TechniquePipeline.SuperKiteStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 3)))
    assert( ! TechniquePipeline.SuperKiteStage.appliesToUnit(TestKit.friendly(UnitType.Protoss_Corsair, 4)))
  }
}
