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

  /**
   * Slide urgency (pure frames): elapsed share of the target domain's
   * cooldown - fresh shots push in, cooling guns hang back. The domain
   * matters, not the max of both: a split-cooldown hull must weight the
   * duel it fights, never the sharper half it cannot use. Floored at
   * one frame so a domain the hull cannot shoot never NaNs the forces.
   */
  def urgency01(cooldownMax: Int, cooldownLeft: Int): Double = {
    val domain = Math.max(1, cooldownMax)
    Math.max(0.0, (domain - cooldownLeft).toDouble / domain)
  }

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    unit.agent.act("Reposition")
    val urgency = urgency01(
      combat.target.map(unit.cooldownMaxAgainst).getOrElse(unit.cooldownMaxAirGround),
      unit.cooldownLeft)
    // Haven't tried this out; might help with sliding into formation
    // engageFormation.flatMap(_(unit)).foreach(f => applyForce(Forces.travel, Potential.towards(unit, f)))
    combat.maneuvers.applyForce(Forces.target,   Potential.towardsTarget(unit) * urgency)
    combat.maneuvers.applyForce(Forces.threat,   Potential.softAvoidThreatRange(unit))
    combat.maneuvers.applyForce(Forces.leaving,  Potential.towards(unit, unit.agent.safety) * combat.ground10 * (1 - urgency))
    combat.maneuvers.moveForcefully()
    true
  }
}
