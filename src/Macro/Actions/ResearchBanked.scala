package Macro.Actions

import Lifecycle.With
import ProxyBwapi.Races.Protoss
import ProxyBwapi.Techs.Techs
import ProxyBwapi.UnitClasses.{UnitClass, UnitClasses}
import ProxyBwapi.Upgrades.Upgrades

/**
 * Banked research: floating deep means the tech line starves, not the
 * army. Past the bank every affordable upgrade and tech whose producer
 * stands gets requested - the gameplans own the early timings, this owns
 * the float. Producers gate the menu, so a pure-ground army never buys
 * air upgrades it cannot use.
 */
object ResearchBanked extends MacroActions {

  /** Banked minerals that mean research waits on nothing. */
  val bankMinerals: Int = 700

  /** Banked gas that means research waits on nothing. */
  val bankGas: Int = 500

  /** Pure bank gate for the suite: research only floats this deep. */
  def banked(minerals: Int, gas: Int): Boolean =
    minerals > bankMinerals && gas > bankGas

  def apply(): Unit = {
    if ( ! banked(With.self.minerals, With.self.gas)) return

    Upgrades.all.view
      .filterNot(u => u == Upgrades.None || u == Upgrades.Unknown)
      .foreach(UpgradeContinuously(_))

    Techs.all.view
      .filterNot(t => t(With.self) || t == Protoss.ArchonMeld || t == Protoss.DarkArchonMeld)
      .filter(t => ours(t.whatResearches) > 0
        && (t.requiredUnit == UnitClasses.None || ours(t.requiredUnit) > 0))
      .foreach(get(_))
  }

  private def ours(unitClass: UnitClass): Int =
    With.macroCounts.oursExtant(unitClass)
}
