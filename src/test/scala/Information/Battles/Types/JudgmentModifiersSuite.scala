package Information.Battles.Types

import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitClasses.UnitClasses
import ProxyBwapi.UnitInfo.{ForeignUnitInfo, FriendlyUnitInfo}
import bwapi.UnitType
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class JudgmentModifiersSuite extends AnyFunSuite {

  private def oursZealot(id: Int): FriendlyUnitInfo =
    TestKit.friendly(UnitType.Protoss_Zealot, id)

  private def oursDragoon(id: Int): FriendlyUnitInfo =
    TestKit.friendly(UnitType.Protoss_Dragoon, id)

  private def foeLing(id: Int): ForeignUnitInfo = {
    val bw = TestKit.mockUnit(UnitType.Zerg_Zergling)
    val unit = new ForeignUnitInfo(bw, id)
    unit.changeUnitType(UnitClasses.get(UnitType.Zerg_Zergling))
    unit
  }

  private def foeHydra(id: Int): ForeignUnitInfo = {
    val bw = TestKit.mockUnit(UnitType.Zerg_Hydralisk)
    val unit = new ForeignUnitInfo(bw, id)
    unit.changeUnitType(UnitClasses.get(UnitType.Zerg_Hydralisk))
    unit
  }

  test("pure zealots against pure lings demand the ten percent margin") {
    TestKit.seedWorld()
    val modifier = JudgmentModifiers.zealotsVsLings(
      Seq(oursZealot(1), oursZealot(2), oursZealot(3)),
      Seq(foeLing(11), foeLing(12)))
    assert(modifier.exists(_.targetDelta == 0.10))
  }

  test("a dragoon on our side lifts the pure-fight caution") {
    TestKit.seedWorld()
    assert(JudgmentModifiers.zealotsVsLings(
      Seq(oursZealot(1), oursDragoon(2)),
      Seq(foeLing(11), foeLing(12))).isEmpty)
  }

  test("a hydra on their side lifts the pure-fight caution") {
    TestKit.seedWorld()
    assert(JudgmentModifiers.zealotsVsLings(
      Seq(oursZealot(1), oursZealot(2)),
      Seq(foeLing(11), foeHydra(12))).isEmpty)
  }

  test("empty sides never trigger the margin") {
    TestKit.seedWorld()
    assert(JudgmentModifiers.zealotsVsLings(Seq.empty, Seq(foeLing(11))).isEmpty)
    assert(JudgmentModifiers.zealotsVsLings(Seq(oursZealot(1)), Seq.empty).isEmpty)
  }

  test("a lone static counts its bare value") {
    assert(JudgmentModifiers.guardedValue(100.0, 0.0) == 100.0)
  }

  test("a screened static counts fifty percent extra") {
    assert(JudgmentModifiers.guardedValue(100.0, 100.0) == 150.0)
    assert(JudgmentModifiers.guardedValue(100.0, 50.0) == 125.0)
  }

  test("a single defender does not screen a static") {
    assert( ! JudgmentModifiers.isGuarded(0))
    assert( ! JudgmentModifiers.isGuarded(1))
    assert(JudgmentModifiers.isGuarded(2))
    assert(JudgmentModifiers.isGuarded(9))
  }
}
