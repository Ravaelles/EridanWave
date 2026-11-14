package Micro.Corsair

import Mathematics.Maff
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.support.SquadAcePilots

/**
 * Corsair brain, ported from Atlantis CorsairBrain: one ordered chain
 * decides what a Corsair does instead of idling. First behavior that
 * claims the unit owns its intents for the frame; the squad's own
 * activities remain the fallback for unclaimed units.
 */
object CorsairBrain {

  def act(squad: SquadAcePilots): Boolean = {
    var claimed = false
    squad.units.foreach(unit => {
      if (CorsairRangedFlee.act(squad, unit)
        || spreadOut(squad, unit)
        || CorsairRelocateWhenIdle.act(squad, unit)
        || CorsairHuntMutas.act(squad, unit)
        || CorsairDanceToOverlord.act(squad, unit)
        || CorsairHunt.act(squad, unit)
        || CorsairHuntKnownOverlords.act(squad, unit)
        || CorsairExploreEnemyMain.act(squad, unit)) {
        claimed = true
      }
    })
    claimed
  }

  /**
   * Wing separation: stacked Corsairs eat the same Scourge, so the newer
   * hull of a too-close pair slides off the ray while the older holds
   * the hunt. Runs after flee (survival first) and before every other
   * claim, since spacing constrains all of them equally.
   */
  private def spreadOut(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit)) return false

    val others = squad.units.view.filter(u => u != unit && Protoss.Corsair(u))
      .map(u => SpreadOutAirUnits.OlderWing(u.id, u.pixel, unit.tileDistanceEdge(u))).toSeq
    SpreadOutAirUnits.partner(unit.id, others).exists(older => {
      if (SpreadOutAirUnits.shouldSpread(unit.id, older.id, older.distTiles)) {
        unit.intend(squad).setTerminus(SpreadOutAirUnits.awayFrom(unit.pixel, older.pixel))
        unit.agent.act("CorsairSpread")
        true
      } else false
    })
  }
}
