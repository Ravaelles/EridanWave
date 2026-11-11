package Micro.Actions.Combat.Decisioning

import Debugging.Visualizations.Forces
import Micro.Heuristics.Potential
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Reposition behavior (was the fallback branch of CombatEngagement.engage):
 * cooling units slide toward the target with urgency while soft-avoiding
 * threat range. Fires when the weapon is not ready to approach with.
 */
object RepositionBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    unit.agent.act("Reposition")
    val urgency01 = Math.max(0.0, (unit.cooldownMaxAirGround - unit.cooldownLeft).toDouble / (unit.cooldownMaxAirGround))
    // Haven't tried this out; might help with sliding into formation
    // engageFormation.flatMap(_(unit)).foreach(f => applyForce(Forces.travel, Potential.towards(unit, f)))
    combat.maneuvers.applyForce(Forces.target,   Potential.towardsTarget(unit) * urgency01)
    combat.maneuvers.applyForce(Forces.threat,   Potential.softAvoidThreatRange(unit))
    combat.maneuvers.applyForce(Forces.leaving,  Potential.towards(unit, unit.agent.safety) * combat.ground10 * (1 - urgency01))
    combat.maneuvers.moveForcefully()
    true
  }
}
