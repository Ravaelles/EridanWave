package Micro.Actions.Combat.Decisioning

import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Attack-if-ready behavior (was CombatEngagement.attackIfReady): issues the
 * attack order the moment the weapon and the approach allow it. Shared by
 * the engage orchestration, abuse positioning and maneuver guards; returns
 * the unit's readiness either way so callers keep their cadence.
 */
object AttackIfReadyBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if (unit.ready && combat.target.exists(unit.readyForAttackOrder) && combat.distances.readyToApproachTarget) {
      Commander.attack(unit)
    }
    unit.unready
  }
}
