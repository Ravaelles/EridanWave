package Micro.Actions.Combat.Decisioning

import Debugging.Visualizations.Forces
import Mathematics.Maff
import Micro.Heuristics.Potential
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Assemble behavior (was the assembling branch of
 * CombatEngagement.engage): pre-fight units hold relative depth along the
 * march axis while hard-avoiding threat range. Marks the combat restrained
 * while the group closes on its mean target distance.
 */
object AssembleBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    unit.agent.act("Assemble")
    val targetRangeDelta  = unit.matchups.pixelsToTargetRange.getOrElse(unit.sightPixels.toDouble) - combat.group.meanAttackerTargetDistance
    val forwardness       = if (unit.flying) 0.0 else Maff.fastTanh11(targetRangeDelta / 96.0)
    combat.maneuvers.applyForce(Forces.travel,     Potential.towards(unit, unit.agent.destinationNext()))
    combat.maneuvers.applyForce(Forces.threat,     Potential.hardAvoidThreatRange(unit, 96.0))
    combat.maneuvers.applyForce(Forces.regrouping, Potential.regroup(unit))
    combat.maneuvers.moveForcefully()
    combat.restrained = combat.target.forall(t => unit.pixelsToGetInRange(t) < unit.pixelsToGetInRangeFrom(t, unit.agent.destinationNext()))
    combat.restrained ||= unit.intent.terminus.exists(p => unit.pixelDistanceTravelling(p) < unit.pixelDistanceTravelling(unit.agent.destinationNext(), p))
    true
  }
}
