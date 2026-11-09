package Micro.Corsair

import Tactic.Squads.SquadAcePilots

/**
 * Corsair brain, ported from Atlantis CorsairBrain: one ordered chain
 * decides what a Corsair does instead of idling. First behavior that
 * claims the unit owns its intents for the frame; the squad's own
 * activities remain the fallback for unclaimed units.
 */
object CorsairBrain {

  def act(squad: SquadAcePilots): Unit = {
    squad.units.foreach(unit => {
      CorsairRelocateWhenIdle.act(squad, unit) ||
      CorsairHuntMutas.act(squad, unit) ||
      CorsairDanceToOverlord.act(squad, unit) ||
      CorsairHunt.act(squad, unit) ||
      CorsairHuntKnownOverlords.act(squad, unit) ||
      CorsairExploreEnemyMain.act(squad, unit)
    })
  }
}
