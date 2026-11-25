package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.SuicideMission

/**
 * Suicide-attack behavior: a suicide unit idle past ten seconds without
 * a started attack charges the nearest enemy, whatever it is and wherever
 * it stands. Runs before the generic idle rule (which needs a free shot
 * to act); together they escalate from safe to forced. Returns true when
 * it attacked or already chases the pick.
 */
object SuicideEngageBehavior {

  def run(unit: FriendlyUnitInfo): Boolean = {
    if ( ! SuicideMission.held(unit)) return false

    if ( ! unit.agent.shouldFight) return false

    // Charge live enemies, not ghosts: remembered fogged contacts read
    // alive, but their pixel is where the enemy was, not where it is -
    // a one-shot unit spends itself milling around a stale blip.
    // Plausible memories still count through the recall horizon.
    val enemy = Maff.minBy(unit.matchups.enemies.filter(e => e.alive && e.likelyStillThere))(_.pixelDistanceEdge(unit))
    if ( ! SuicideEngage.shouldForceAttack(With.framesSince(unit.lastFrameStartingAttack), enemy.isDefined)) return false

    NearestShot.take(unit, "SuicideEngage", enemy.toSeq)
  }
}
