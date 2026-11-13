package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import Micro.Agency.Commander
import Micro.Actions.Combat.Decisioning.Kiting.{FacingAway, HoldToShoot}
import Micro.Targeting.FiltersSituational.TargetFilterPotshot
import Micro.Targeting.Target
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Potshot behavior (was CombatEngagement.potshot): opportunistic fire at
 * the nearest threat, the current target, or the best potshot target, when
 * the order is ready to land. Returns the unit's readiness either way so
 * callers keep their cadence.
 */
object PotshotBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if (unit.ready) {
      if (Protoss.Dragoon(unit) && vulturePotshot(unit)) return unit.unready

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

  /**
   * Dragoon anti-Vulture potshot: a Vulture closing head-on inside our
   * reach plus the bonus eats the shot the moment the gun is nearly ready
   * instead of donating kite ground to the fastest unit in the game.
   * Runs before the generic pick so the Vulture always wins the duel.
   */
  private def vulturePotshot(unit: FriendlyUnitInfo): Boolean = {
    Maff.minBy(unit.matchups.threats.filter(t =>
      Terran.Vulture(t)
      && t.visible
      && unit.canAttack(t)
      && HoldToShoot.shouldPotshotVulture(
        distPx       = unit.pixelDistanceEdge(t),
        ownRangePx   = unit.pixelRangeAgainst(t),
        cooldownLeft = unit.cooldownLeft,
        foeFacingUs  = FacingAway.facingUs(t.angleRadians, t.pixel.radiansTo(unit.pixel)))))(
      unit.pixelDistanceEdge(_)).exists(vulture => {
      unit.agent.toAttack = Some(vulture)
      unit.agent.act("PotshotVulture")
      Commander.attack(unit)
      true
    })
  }
}
