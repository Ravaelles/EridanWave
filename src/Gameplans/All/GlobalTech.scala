package Gameplans.All

import Gameplans.Protoss.PvZ.StormResearch
import Lifecycle.With
import Macro.Actions.MacroActions
import Macro.Facts.UnitArchive
import ProxyBwapi.Races.Protoss

/**
 * Cross-cutting tech intents, evaluated for every gameplan every game:
 * the "don't forget" invariants no single opening owns. Each rule carries
 * its own conservative kernel gate, production dedups repeated requests,
 * and UMS micro maps opt out (no economy to spend). Add rules here, never
 * by copying them into twenty gameplans.
 */
object GlobalTech extends MacroActions {

  def run(): Unit = {
    if (With.umsMode) return

    stormForTemplars()
  }

  private def stormForTemplars(): Unit = {
    if ( ! With.self.isProtoss) return

    if ( ! have(Protoss.TemplarArchives)) return

    if (StormResearch.shouldResearch(
      UnitArchive.producedTotal(Protoss.HighTemplar),
      unitsComplete(Protoss.HighTemplar),
      units(Protoss.HighTemplar),
      gas)) {
      get(Protoss.PsionicStorm)
    }
  }
}
