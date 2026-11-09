package Micro.Corsair

import Lifecycle.With
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.SquadAcePilots

/**
 * Confirm the enemy main a few seconds per half minute: an unseen main too
 * long means the army fights blind.
 */
object CorsairExploreEnemyMain {

  def applies(unit: FriendlyUnitInfo): Boolean =
    (With.frame / 24) % 30 <= 9 && With.scouting.enemyMain.exists(main =>
      With.framesSince(main.lastFrameScoutedByUs) > 20 * 24)

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit) || ! applies(unit)) {
      false
    } else {
      With.scouting.enemyMain.exists(main => {
        unit.intend(squad).setTargets(Iterable.empty).setTerminus(main.heart.center)
        unit.agent.act("CorsairExploreMain")
        true
      })
    }
  }
}
