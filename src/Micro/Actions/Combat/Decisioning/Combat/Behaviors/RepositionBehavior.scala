package Micro.Actions.Combat.Decisioning

import Debugging.Visualizations.Forces
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Kiting.{KiteAway, KiteHold}
import Micro.Agency.Commander
import Micro.Heuristics.Potential
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Reposition behavior (was the fallback branch of CombatEngagement.engage):
 * cooling units slide toward the target with urgency while soft-avoiding
 * threat range; loaded hulls with something hittable shoot instead of
 * shuffling. Fires when the weapon is not ready to approach with, or
 * loaded with a hittable target and no retreat to run.
 */
object RepositionBehavior {

  /**
   * Slide urgency (pure frames): elapsed share of the target domain's
   * cooldown - fresh shots push in, cooling guns hang back. The domain
   * matters, not the max of both: a split-cooldown hull must weight the
   * duel it fights, never the sharper half it cannot use. Floored at
   * one frame so a domain the hull cannot shoot never NaNs the forces.
   */
  def urgency01(cooldownMax: Int, cooldownLeft: Int): Double = {
    val domain = Math.max(1, cooldownMax)
    Math.max(0.0, (domain - cooldownLeft).toDouble / domain)
  }

  /**
   * Terminal volley (pure flags and frames for the suite): a repositioning
   * hull with a loaded gun and something hittable shoots instead of
   * shuffling - standing volleys used to die by design here (the
   * terminal only moved), never by cooldown. Retreating hulls keep
   * moving: the fallback potshot owns their shots, never this line.
   * This is the test that would have caught the silent duel. Answers
   * through the shared volley verdict now - one question, one owner.
   */
  def shouldVolley(retreating: Boolean, cooldownLeft: Int, hasHittable: Boolean): Boolean =
    VolleyDiscipline.shouldVolley(retreating, cooldownLeft, hasHittable)

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    unit.agent.act("Reposition")
    // Bleeding hulls never start that volley with blades on top (owner
    // order, 1DragWounded): the fallback potshot below still spends
    // aimed shots, but the standing hold yields to footwork instead -
    // and only at the edge, never into contact.
    val nearestMeleeTiles = Maff.minBy(unit.matchups.threats.view.filter(t => t.likelyStillThere && t.unitClass.melee))(
      unit.pixelDistanceEdge(_)).map(unit.pixelDistanceEdge(_) * Maff.inv32).getOrElse(Double.PositiveInfinity)
    val atEdge = combat.target.exists(t => KiteAway.holdsTheEdge(unit.pixelDistanceEdge(t) * Maff.inv32, unit.pixelRangeAgainst(t) * Maff.inv32))
    if (shouldVolley(retreating = combat.shouldRetreat, cooldownLeft = unit.cooldownLeft, hasHittable = unit.matchups.targetsInRange.nonEmpty) && KiteHold.holdsShot(unit.totalHealth, nearestMeleeTiles) && KiteHold.plantsFromEdge(unit.totalHealth, nearestMeleeTiles, atEdge)) {
      if (AttackIfReadyBehavior.run(unit, combat)) return true
      // Loaded and hittable but not yet aimed: hold the volley instead
      // of footworking past it - same rule as the dance and the charge.
      Commander.hold(unit)

      return true
    }

    val urgency = urgency01(
      combat.target.map(unit.cooldownMaxAgainst).getOrElse(unit.cooldownMaxAirGround),
      unit.cooldownLeft)
    // Haven't tried this out; might help with sliding into formation
    // engageFormation.flatMap(_(unit)).foreach(f => applyForce(Forces.travel, Potential.towards(unit, f)))
    combat.maneuvers.applyForce(Forces.target,   Potential.towardsTarget(unit) * urgency)
    combat.maneuvers.applyForce(Forces.threat,   Potential.softAvoidThreatRange(unit))
    combat.maneuvers.applyForce(Forces.leaving,  Potential.towards(unit, unit.agent.safety) * combat.ground10 * (1 - urgency))
    combat.maneuvers.moveForcefully()
    true
  }
}
