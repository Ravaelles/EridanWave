package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import Micro.Agency.Commander
import Micro.Targeting.FiltersSituational.TargetFilterPotshot
import Micro.Targeting.Target
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Potshot behavior (was CombatEngagement.potshot): opportunistic fire at
 * the nearest threat, the current target, or the best potshot target, when
 * the order is ready to land. Returns the unit's readiness either way so
 * callers keep their cadence.
 */
object PotshotBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if (unit.ready) {
      val potshotTarget = Maff.orElseFiltered(
          Maff.minBy(unit.matchups.threats)(_.pixelDistanceEdge(unit)).filter(TargetFilterPotshot.legal(unit, _)),
          combat.target.filter(TargetFilterPotshot.legal(unit, _)),
          Target.best(unit, TargetFilterPotshot))(unit.canAttack).headOption
      if (potshotTarget.exists(t => unit.readyForAttackOrder(t) && TargetFilterPotshot.legal(unit, t))) {
        unit.agent.toAttack = potshotTarget
        unit.agent.act("Potshot")
        Commander.attack(unit)
      }
    }
    unit.unready
  }
}
