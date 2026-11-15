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

  def run(unit: FriendlyUnitInfo, situation: CombatSituation): Boolean = {
    if (unit.ready && situation.target.exists(unit.readyForAttackOrder) && situation.distances.readyToApproachTarget) {
      Commander.attack(unit)
    }
    unit.unready
  }
}
