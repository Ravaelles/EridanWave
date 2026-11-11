package Micro.Actions.Combat.Decisioning

import Micro.Actions.Combat.Maneuvering.Retreat
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.UnitFilters.IsWarrior

/**
 * Abuse behavior (was CombatEngagement.abuse): outranged units keep a
 * minimum space from the deepest threat and retreat from anything closing
 * on them, then chase at the ideal forward distance. Returns true always;
 * it positions rather than decides.
 */
object AbuseBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    var minimumSpace = 16.0
    unit.matchups.threatDeepest.foreach(t => {
      if (IsWarrior(t)) {
        minimumSpace += 32
        if (t.presumptiveTarget.contains(unit)) {
          minimumSpace += 32
        }
        if (t.topSpeed > unit.topSpeed) {
          minimumSpace += 32
        }
      }
      lazy val rangeDifferential = unit.pixelRangeAgainst(t) - t.pixelRangeAgainst(unit)
      if (unit.canAttack(t) && rangeDifferential > 0) {
        minimumSpace = Math.min(minimumSpace, rangeDifferential / 2)
      }
    })
    if (combat.target.map(unit.readyForAttackOrder).getOrElse(unit.readyForAttackOrder)) {
      minimumSpace = Math.min(minimumSpace, combat.target.map(unit.pixelRangeAgainst).getOrElse(unit.pixelRangeMax) - 24)
    }

    if (unit.matchups.threatDeepest.exists(_.pixelsToGetInRange(unit) < minimumSpace)) {
      Retreat(unit)
    }
    if ( ! AttackIfReadyBehavior.run(unit, combat) && unit.matchups.threatDeepest.exists(a => a.presumptiveTarget.contains(unit) || a.speedApproaching(unit) > 0)) {
      Retreat(unit)
    }
    if (combat.idealDistanceForward > 0) {
      combat.maneuvers.chase()
    }
    combat.maneuvers.charge()
    unit.unready
  }
}
