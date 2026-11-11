package Micro.Actions.Combat.Decisioning

import Debugging.Visualizations.Forces
import Lifecycle.With
import Micro.Actions.Combat.Decisioning.Kiting.CorsairSafety
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Micro.Heuristics.Potential
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Superkite behavior (was CombatEngagement.superkite): attack-flee cycling
 * against a single target. Kites while the shot is not ready or safety
 * runs short; chases while closing; attacks the moment the order lands.
 * Returns true always; it positions rather than decides.
 */
object SuperkiteBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    combat.target.foreach(t => {
      var kite  = false
      var chase = false
      if (unit.readyForAttackOrder(t)) {
        if (unit.framesToGetInRange(t) < With.game.getRemainingLatencyFrames) {
          if (unit.orderTarget.contains(t)) {
            kite = true
          } else {
            Commander.attack(unit)
          }
        } else {
          chase = true
        }
      } else if (unit.matchups.framesOfSafety < unit.cooldownLeft + unit.cooldownMaxAgainst(t)) {
        kite = true
      }
      if (kite) {
        unit.agent.forces(Forces.threat)      = Potential.hardAvoidThreatRange(unit, CorsairSafety.avoidanceMarginPixels(unit))
        unit.agent.forces(Forces.regrouping)  = Potential.regroup(unit)
        MicroPathing.moveForcefully(unit)
      } else if (chase) {
        unit.agent.forces(Forces.target)      = Potential.towards(unit, t.pixel)
        unit.agent.forces(Forces.regrouping)  = Potential.regroup(unit) * 0.5
        MicroPathing.moveForcefully(unit)
      }
    })

    unit.unready
  }
}
