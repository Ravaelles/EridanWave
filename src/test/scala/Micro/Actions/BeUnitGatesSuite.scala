package Micro.Actions

import Micro.Actions.Protoss.Air.BeFlier
import Micro.Actions.Protoss.Arbiter.BeArbiter
import Micro.Actions.Protoss.Carrier.BeCarrier
import Micro.Actions.Protoss.DarkTemplar.BeDarkTemplar
import Micro.Actions.Protoss.Detector.BeDetector
import Micro.Actions.Protoss.Dragoon.BeDragoonVsSlowlot
import Micro.Actions.Protoss.ReaverShuttle.{BeReaver, BeShuttle}
import Micro.Actions.Protoss.Zealot.{BeRushingZealot, BeZealotVsZealot}
import Micro.Actions.Terran.{BeCombatSCV, BeVulture}
import Micro.Actions.Zerg.BeMutalisk
import bwapi.UnitType
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

/**
 * Legacy-brain unit gates (P6): every Be* brain states which hulls it may
 * ever apply to, beside its execute - the same contract behavior stages
 * carry. Game and map reads (frame, matchups, intents, squads) stay in
 * allowed; the gate answers only race and class, ground or air, mobile
 * or held. The run keeps its own checks for direct callers.
 *
 * Registry: adding a Be* brain adds a row below. Leaves claim their
 * frames under their own name (no silent orders); the pure decider
 * (BeZealotVsZealot) votes and never claims. Mocks read grounded, so
 * airborne positives stay live-only and the flier row pins negatives.
 */
class BeUnitGatesSuite extends AnyFunSuite {

  test("dragoon slowlot brain: dragoons only, claims its frames") {
    TestKit.seedWorld()
    assert(BeDragoonVsSlowlot.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! BeDragoonVsSlowlot.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 2)))
    assert( ! BeDragoonVsSlowlot.isContainer)
  }

  test("rushing zealot brain: zealots only, claims its frames") {
    TestKit.seedWorld()
    assert(BeRushingZealot.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 1)))
    assert( ! BeRushingZealot.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 2)))
    assert( ! BeRushingZealot.isContainer)
  }

  test("zealot duel brain: zealots only, votes without claiming") {
    TestKit.seedWorld()
    assert(BeZealotVsZealot.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 1)))
    assert( ! BeZealotVsZealot.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 2)))
    assert( ! BeZealotVsZealot.isContainer)
  }

  test("carrier brain: carriers only, claims its frames") {
    TestKit.seedWorld()
    assert(BeCarrier.appliesToUnit(TestKit.friendly(UnitType.Protoss_Carrier, 1)))
    assert( ! BeCarrier.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 2)))
    assert( ! BeCarrier.isContainer)
  }

  test("arbiter brain: arbiters only, claims its frames") {
    TestKit.seedWorld()
    assert(BeArbiter.appliesToUnit(TestKit.friendly(UnitType.Protoss_Arbiter, 1)))
    assert( ! BeArbiter.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 2)))
    assert( ! BeArbiter.isContainer)
  }

  test("reaver brain: reavers only, claims its frames") {
    TestKit.seedWorld()
    assert(BeReaver.appliesToUnit(TestKit.friendly(UnitType.Protoss_Reaver, 1)))
    assert( ! BeReaver.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 2)))
    assert( ! BeReaver.isContainer)
  }

  test("shuttle brain: shuttles only, claims its frames") {
    TestKit.seedWorld()
    assert(BeShuttle.appliesToUnit(TestKit.friendly(UnitType.Protoss_Shuttle, 1)))
    assert( ! BeShuttle.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 2)))
    assert( ! BeShuttle.isContainer)
  }

  test("dark templar brain: blades only, claims its frames") {
    TestKit.seedWorld()
    assert(BeDarkTemplar.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dark_Templar, 1)))
    assert( ! BeDarkTemplar.appliesToUnit(TestKit.friendly(UnitType.Protoss_Zealot, 2)))
    assert( ! BeDarkTemplar.isContainer)
  }

  test("detector brain: mobile eyes only, claims its frames") {
    TestKit.seedWorld()
    assert(BeDetector.appliesToUnit(TestKit.friendly(UnitType.Protoss_Observer, 1)))
    assert( ! BeDetector.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 2)))
    assert( ! BeDetector.isContainer)
  }

  test("flier steering: never grounds, buildings or transports; mocks read grounded") {
    TestKit.seedWorld()
    assert( ! BeFlier.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 1)))
    assert( ! BeFlier.appliesToUnit(TestKit.friendly(UnitType.Protoss_Pylon, 2)))
    assert( ! BeFlier.appliesToUnit(TestKit.friendly(UnitType.Protoss_Shuttle, 3)))
    assert( ! BeFlier.isContainer)
  }

  test("vulture brain: vultures only, claims its frames") {
    TestKit.seedWorld()
    assert(BeVulture.appliesToUnit(TestKit.friendly(UnitType.Terran_Vulture, 1)))
    assert( ! BeVulture.appliesToUnit(TestKit.friendly(UnitType.Terran_SCV, 2)))
    assert( ! BeVulture.isContainer)
  }

  test("combat scv brain: scvs only, claims its frames") {
    TestKit.seedWorld()
    assert(BeCombatSCV.appliesToUnit(TestKit.friendly(UnitType.Terran_SCV, 1)))
    assert( ! BeCombatSCV.appliesToUnit(TestKit.friendly(UnitType.Terran_Vulture, 2)))
    assert( ! BeCombatSCV.isContainer)
  }

  test("mutalisk brain: mutalisks only, claims its frames") {
    TestKit.seedWorld()
    assert(BeMutalisk.appliesToUnit(TestKit.friendly(UnitType.Zerg_Mutalisk, 1)))
    assert( ! BeMutalisk.appliesToUnit(TestKit.friendly(UnitType.Protoss_Dragoon, 2)))
    assert( ! BeMutalisk.isContainer)
  }
}
