package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Agency.Commander
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
    if (hittable.isEmpty) return false

    if (unit.matchups.threatsInRange.nonEmpty) return false

    if (With.framesSince(unit.lastFrameStartingAttack) <= IdleAttack.idleFrames) return false


    val target = Maff.minBy(hittable)(unit.pixelDistanceEdge(_)).get
    if ( ! unit.orderTarget.contains(target)) {
      unit.agent.act("IdleEngage")
      Commander.attack(unit, target)
    }

    true
  }
}
