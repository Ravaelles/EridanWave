package Gameplans.All

import Gameplans.Protoss.PvZ.policy.StormResearch
import Lifecycle.With
import Macro.Actions.MacroActions
import Macro.Facts.UnitArchive
import Mathematics.Maff
import Placement.Access.PlaceLabels
import ProxyBwapi.Races.{Protoss, Zerg}

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
    cannonsVsMutalisks()
    batteriesForCarriers()
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

  // Shown Mutalisks kill unguarded naturals: Forge plus two-to-five
  // DefendAir cannons at every base, scaled with the flock. Prerequisites
  // and repeat requests dedup in production; shown (not likely) fires it.
  private def cannonsVsMutalisks(): Unit = {
    if ( ! With.self.isProtoss) return
    if ( ! With.enemies.exists(_.isZerg)) return
    if ( ! enemyHasShown(Zerg.Mutalisk)) return

    buildCannonsAtBases(Maff.clamp(1 + enemies(Zerg.Mutalisk) / 3, 2, 5), PlaceLabels.DefendAir)
  }

  // Carrier wings recharge at home: eight interceptors earn a battery,
  // twenty earn a second. First ideally at a third mining base, else at
  // the main; the second takes the main when the third already holds one.
  // Prerequisites and repeat requests dedup in production.
  private def batteriesForCarriers(): Unit = {
    if ( ! With.self.isProtoss) return

    val want = CarrierBatteryDemand.wantBatteries(units(Protoss.Interceptor))
    if (want == 0) return

    val main = With.geography.ourMain
    val third = With.geography.ourMiningBases.find(b => ! b.isOurMain && ! b.isOurNatural)
    val spots = third.toSeq :+ main
    val have = With.units.ours.count(u => u.alive && Protoss.ShieldBattery(u) && u.base.exists(spots.contains))
    spots
      .find(spot => With.units.ours.count(u => u.alive && Protoss.ShieldBattery(u) && u.base.contains(spot)) == 0)
      .takeWhile(_ => have < want)
      .foreach(spot => buildDefensesAt(1, Protoss.ShieldBattery, Seq(PlaceLabels.DefendHall), Seq(spot)))
  }

}
