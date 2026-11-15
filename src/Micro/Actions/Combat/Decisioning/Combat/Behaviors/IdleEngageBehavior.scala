package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Idle-attack behavior: a combat unit sitting on a hittable enemy with
 * nobody able to hit it back shoots instead of holding a pose it has
 * held for hundreds of frames. Last resort before the reposition dance -
 * kiting, dressing and valid holds all outrank it. Returns true when it
 * attacked (or already is), so the dispatch never falls through to a
 * reposition that would restart the idle wait.
 */
object IdleEngageBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! unit.agent.shouldFight) return false

    if ( ! unit.unitClass.isWarrior) return false

    val hittable = unit.matchups.targetsInRange
    if ( ! IdleAttack.shouldForceAttack(
      framesSinceAttack = With.framesSince(unit.lastFrameStartingAttack),
      hasHittableEnemy  = hittable.nonEmpty,
      shouldFight       = unit.agent.shouldFight,
      locallyAhead      = unit.matchups.threatsInRange.isEmpty)) return false

    NearestShot.take(unit, "IdleEngage", hittable)
  }
}
