package Micro.Actions.Combat.Decisioning

import Micro.Agency.Commander
import Micro.Targeting.FiltersSituational.TargetFilterVisibleInRange
import Micro.Targeting.Target
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Aim behavior (was CombatEngagement.aim): picks a visible in-range target
 * and issues attack plus hold. Returns the unit's readiness either way so
 * callers keep their cadence.
 */
object AimBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if (unit.ready) {
      Target.choose(unit, TargetFilterVisibleInRange)
      Commander.attack(unit)
      Commander.hold(unit)
    }
    unit.unready
  }
}
