package Micro.Actions.Combat.Decisioning

import Micro.Actions.Combat.Maneuvering.Retreat
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import ProxyBwapi.UnitInfo.UnitInfo
import Utilities.UnitFilters.IsWarrior

/**
 * Abuse behavior (was CombatEngagement.abuse): outranged units keep a
 * minimum space from the deepest threat and retreat from anything closing
 * on them, then chase at the ideal forward distance. Returns true always;
 * it positions rather than decides.
 */
object AbuseBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    val minimumSpace = minimumSpaceFor(unit, combat)

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

  private def minimumSpaceFor(unit: FriendlyUnitInfo, combat: Combat): Double = {
    var space = baseSpace(unit)

    if (combat.target.map(unit.readyForAttackOrder).getOrElse(unit.readyForAttackOrder)) {
      space = Math.min(space, combat.target.map(unit.pixelRangeAgainst).getOrElse(unit.pixelRangeMax) - 24)
    }

    space
  }

  private def baseSpace(unit: FriendlyUnitInfo): Double = {
    var minimumSpace = 16.0
    unit.matchups.threatDeepest.foreach(t => {
      minimumSpace = widenForWarrior(unit, t, minimumSpace)
      lazy val rangeDifferential = unit.pixelRangeAgainst(t) - t.pixelRangeAgainst(unit)
      if (unit.canAttack(t) && rangeDifferential > 0) {
        minimumSpace = Math.min(minimumSpace, rangeDifferential / 2)
      }
    })

    minimumSpace
  }

  private def widenForWarrior(unit: FriendlyUnitInfo, t: UnitInfo, current: Double): Double = {
    var space = current
    if (IsWarrior(t)) {
      space += 32
      if (t.presumptiveTarget.contains(unit)) {
        space += 32
      }
      if (t.topSpeed > unit.topSpeed) {
        space += 32
      }
    }

    space
  }
}
