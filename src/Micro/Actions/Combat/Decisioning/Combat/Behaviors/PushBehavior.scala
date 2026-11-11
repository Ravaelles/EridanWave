package Micro.Actions.Combat.Decisioning

import Debugging.Visualizations.Forces
import Micro.Heuristics.Potential
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Push behavior (was the pushThrough branch of CombatEngagement.engage):
 * shoved units drive through the line toward station or perch while
 * soft-avoiding threat range. Fires only on an explicit traffic push.
 */
object PushBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    unit.agent.act("Push")
    combat.maneuvers.applySeparationForces()
    if (unit.agent.forces(Forces.spacing).lengthFast > 0 || ! AttackIfReadyBehavior.run(unit, combat)) {
      combat.maneuvers.applyForce(Forces.travel, Potential.towards(unit, unit.agent.station.pixel.orElse(unit.agent.perch.pixel).getOrElse(unit.agent.destinationNext())))
      combat.maneuvers.applyForce(Forces.target, Potential.towards(unit,                                 unit.agent.perch.pixel .getOrElse(unit.agent.destinationNext())))
      combat.maneuvers.applyForce(Forces.threat, Potential.softAvoidThreatRange(unit) * 1.5)
      combat.maneuvers.moveForcefully()
    }
    true
  }
}
