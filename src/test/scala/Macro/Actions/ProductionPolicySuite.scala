package Macro.Actions

import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitClasses.UnitClass
import org.scalatest.funsuite.AnyFunSuite

class ProductionPolicySuite extends AnyFunSuite {

  private def withProduction[T](reaver: Boolean)(body: => T): T = {
    val old = Strategery.StrategyToggles.reaverProductionEnabled
    Strategery.StrategyToggles.reaverProductionEnabled = reaver
    try body finally Strategery.StrategyToggles.reaverProductionEnabled = old
  }

  private def facts(
    dragoons: Int = 0,
    enemyIsProtoss: Boolean = true,
    enemyCannonKnown: Boolean = false,
    haveReaver: Boolean = false,
    haveBay: Boolean = false): ProductionPolicy.Facts =
    ProductionPolicy.Facts(dragoons, enemyIsProtoss, enemyCannonKnown, haveReaver, haveBay)

  test("the first Reaver waits for fifteen Dragoons") {
    withProduction(reaver = true) {
      assert(ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 0)).isDefined)
      assert(ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 14)).isDefined)
      assert(ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 15)).isEmpty)
    }
  }

  test("a scouted Cannon answers without the backbone") {
    withProduction(reaver = true) {
      assert(ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 0, enemyCannonKnown = true)).isEmpty)
    }
  }

  test("the Bay waits with the first hull in a mirror") {
    withProduction(reaver = true) {
      assert(ProductionPolicy.veto(Protoss.RoboticsSupportBay, facts(dragoons = 3)).isDefined)
      assert(ProductionPolicy.veto(Protoss.RoboticsSupportBay, facts(dragoons = 15)).isEmpty)
    }
  }

  test("committed play flows") {
    withProduction(reaver = true) {
      assert(ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 0, haveReaver = true)).isEmpty)
      assert(ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 0, haveBay = true)).isEmpty)
      assert(ProductionPolicy.veto(Protoss.RoboticsSupportBay, facts(dragoons = 0, haveBay = true)).isEmpty)
    }
  }

  test("other matchups and classes always flow") {
    withProduction(reaver = true) {
      assert(ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 0, enemyIsProtoss = false)).isEmpty)
      assert(ProductionPolicy.veto(Protoss.RoboticsSupportBay, facts(dragoons = 0, enemyIsProtoss = false)).isEmpty)
      assert(ProductionPolicy.veto(Protoss.Dragoon, facts(dragoons = 0)).isEmpty)
      assert(ProductionPolicy.veto(Protoss.Gateway, facts(dragoons = 0)).isEmpty)
      assert(ProductionPolicy.veto(Protoss.Probe, facts(dragoons = 0)).isEmpty)
    }
  }

  test("the veto carries its reason") {
    withProduction(reaver = true) {
      val veto = ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 3))
      assert(veto.exists(_.label == "dragoons 3 < 15"))
    }
  }

  test("a disabled Reaver track vetoes hull and Bay with its reason") {
    withProduction(reaver = false) {
      assert(ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 30)).isDefined)
      assert(ProductionPolicy.veto(Protoss.RoboticsSupportBay, facts(dragoons = 30)).isDefined)
      assert(ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 30)).exists(_.label == "disabled: reaver production"))
    }
  }

  test("a disabled track still flows committed and foreign play") {
    withProduction(reaver = false) {
      assert(ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 0, haveBay = true)).isEmpty)
      assert(ProductionPolicy.veto(Protoss.Reaver, facts(dragoons = 0, enemyIsProtoss = false)).isEmpty)
      assert(ProductionPolicy.veto(Protoss.Dragoon, facts(dragoons = 0)).isEmpty)
    }
  }
}
