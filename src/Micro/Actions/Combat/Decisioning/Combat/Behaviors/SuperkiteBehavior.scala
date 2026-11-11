package Micro.Actions.Combat.Decisioning

import Debugging.Visualizations.Forces
import Lifecycle.With
import Micro.Actions.Combat.Decisioning.Kiting.CorsairSafety
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Micro.Heuristics.Potential
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import ProxyBwapi.UnitInfo.UnitInfo

/**
 * Superkite behavior (was CombatEngagement.superkite): attack-flee cycling
 * against a single target. Kites while the shot is not ready or safety
 * runs short; chases while closing; attacks the moment the order lands.
 * Returns true always; it positions rather than decides.
 */
object SuperkiteBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    combat.target.foreach(t => cycle(unit, t))

    unit.unready
  }

  private def cycle(unit: FriendlyUnitInfo, t: UnitInfo): Unit = {
    val (kite, chase) = kiteOrChase(unit, t)

    applyKiteChase(unit, t, kite, chase)
  }

  private def kiteOrChase(unit: FriendlyUnitInfo, t: UnitInfo): (Boolean, Boolean) = {
    if (unit.readyForAttackOrder(t)) {
      readyDuel(unit, t)
    } else if (unit.matchups.framesOfSafety < unit.cooldownLeft + unit.cooldownMaxAgainst(t)) {
      (true, false)
    } else {
      (false, false)
    }
  }

  private def readyDuel(unit: FriendlyUnitInfo, t: UnitInfo): (Boolean, Boolean) = {
    if (unit.framesToGetInRange(t) < With.game.getRemainingLatencyFrames) {
      if (unit.orderTarget.contains(t)) {
        (true, false)
      } else {
        Commander.attack(unit)
        (false, false)
      }
    } else {
      (false, true)
    }
  }

  private def applyKiteChase(unit: FriendlyUnitInfo, t: UnitInfo, kite: Boolean, chase: Boolean): Unit = {
    if (kite) {
      unit.agent.forces(Forces.threat)      = Potential.hardAvoidThreatRange(unit, CorsairSafety.avoidanceMarginPixels(unit))
      unit.agent.forces(Forces.regrouping)  = Potential.regroup(unit)
      MicroPathing.moveForcefully(unit)
    } else if (chase) {
      unit.agent.forces(Forces.target)      = Potential.towards(unit, t.pixel)
      unit.agent.forces(Forces.regrouping)  = Potential.regroup(unit) * 0.5
      MicroPathing.moveForcefully(unit)
    }
  }
}
