package Micro.Actions.Combat.Decisioning

import Debugging.Visualizations.Forces
import Micro.Coordination.Pushing.TrafficPriorities
import Micro.Heuristics.Potential
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Time.Forever

/**
 * Scavenge behavior (was the Scavenge branch of CombatEngagement.engage):
 * opportunistic sniping from safety. Potshots when the order lands, else
 * lurks toward the target while hard-avoiding threat range by attack time.
 */
object ScavengeBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! PotshotBehavior.run(unit, combat)) {
      unit.agent.act("Snipe")
      val framesSafe      = unit.matchups.threatSoonest.map(_.framesToLaunchAttack(unit)).getOrElse(Forever())
      val framesToAttack  = combat.target.map(unit.framesToLaunchAttack).getOrElse(0)
      if (framesToAttack >= framesSafe || ! AttackIfReadyBehavior.run(unit, combat)) {
        unit.agent.act("Lurk")
        unit.agent.escalatePriority(TrafficPriorities.Bump) // Stronger than attackers, less than full retreaters
        combat.maneuvers.applyForce(Forces.target,   Potential.towardsTarget(unit))
        combat.maneuvers.applyForce(Forces.threat,   Potential.hardAvoidThreatRange(unit, Math.max(unit.topSpeed * framesToAttack, 64.0)))
        combat.maneuvers.applyForce(Forces.leaving,  Potential.towards(unit, unit.agent.safety) * combat.ground10)
        combat.maneuvers.moveForcefully()
      }
    }
    true
  }
}
