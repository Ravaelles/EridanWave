package Micro.Actions.Combat.Decisioning

import Debugging.Visualizations.Forces
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Actions.Combat.Decisioning.Skirmish.FightRules
import Micro.Actions.Combat.Decisioning.Formation.{FormationApproach, FormationHold, OverwhelminglyStronger}
import Micro.Actions.Combat.Decisioning.Kiting.{CorsairSafety, KiteAway}
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Micro.Coordination.Pushing.TrafficPriorities
import Micro.Heuristics.Potential
import Micro.Targeting.FiltersSituational.{TargetFilterPotshot, TargetFilterVisibleInRange}
import Micro.Targeting.Target
import ProxyBwapi.Races.{Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Micro.Actions.Combat.Maneuvering.Retreat
import Utilities.Time.Forever
import Utilities.UnitFilters.IsWarrior
import Utilities.?

class CombatEngagement(combat: Combat, unit: FriendlyUnitInfo) {

  private[Decisioning] def engage(): Boolean = {
    if (unit.ready) {
      combat.breakFormationToAttack  = unit.formationEngage.isEmpty
      combat.breakFormationToAttack ||= combat.target.exists(targ =>
        // Maintain formation if we're not ready to attack yet
        combat.distances.readyToApproachTarget
        // Maintain formation until arriving at a fight or the destinatation
        && (unit.battle.isDefined || unit.pixelDistanceEdge(targ) <= unit.sightPixels || (unit.pixelDistanceEdge(targ) <= 256 && targ.visible))
        // Maintain formation while flanking
        && ! unit.formationEngage.exists(_.flanking)
        // Maintain formation until battle breaks out
        && ( ! combat.formationHelpsEngage || combat.squadEngagedUpon))
      combat.breakFormationToAttack ||= ! unit.formationEngage.exists(_.flanking) && combat.target.exists(unit.pixelsToGetInRange(_) < Math.max(0, 128 * unit.confidence11))
      combat.breakFormationToAttack ||= With.yolo.active

      // Hold the line while a ranged armed combat target is well beyond our
      // firing range (tiles = pixels / 32, as in Atlantis): keep formation
      // slots and let the enemy come to us instead of chasing piecemeal.
      // Buildings - even shooting ones - and unarmed units are always
      // attacked normally: there is no line to hold against them.
      // Inside ~1 tile of the range the fight starts - the unit steps in and
      // shoots. The hold also releases once the fight is on: a cluster mate
      // engaging, or an enemy in range whose weapon is cooling down (it opened
      // fire). Melee must still close, flankers must still circle, and an
      // explicit yolo overrules.
      if ( ! OverwhelminglyStronger.skipFormation(unit) && ! With.yolo.active && combat.target.exists(t =>
        FormationHold.shouldHoldLine(
          tilesBeyondRange  = unit.pixelsToGetInRange(t) / 32.0,
          melee             = unit.unitClass.melee,
          hasFormation      = unit.formationEngage.isDefined,
          flanking          = unit.formationEngage.exists(_.flanking),
          allyEngaged       = combat.squadEngagingOn,
          enemyFired        = unit.matchups.threatsInRange.exists(_.cooldownLeft > 0),
          targetHoldable    = t.unitClass.isWarrior && t.unitClass.canAttack && (! t.unitClass.isBuilding || t.unitClass.isCombatBuilding)))) {
        combat.breakFormationToAttack = false
      }

      if ( ! combat.breakFormationToAttack) {
        unit.agent.perch.clear()
      }
      if (combat.pushThrough) {
        unit.agent.act("Push")
        combat.maneuvers.applySeparationForces()
        if (unit.agent.forces(Forces.spacing).lengthFast > 0 || ! AttackIfReadyBehavior.run(unit, combat)) {
          combat.maneuvers.applyForce(Forces.travel, Potential.towards(unit, unit.agent.station.pixel.orElse(unit.agent.perch.pixel).getOrElse(unit.agent.destinationNext())))
          combat.maneuvers.applyForce(Forces.target, Potential.towards(unit,                                 unit.agent.perch.pixel .getOrElse(unit.agent.destinationNext())))
          combat.maneuvers.applyForce(Forces.threat, Potential.softAvoidThreatRange(unit) * 1.5)
          combat.maneuvers.moveForcefully()
        }
      } else if (combat.assembling && combat.group.groupUnits.size > 1) {
        unit.agent.act("Assemble")
        val targetRangeDelta  = unit.matchups.pixelsToTargetRange.getOrElse(unit.sightPixels.toDouble) - combat.group.meanAttackerTargetDistance
        val forwardness       = if (unit.flying) 0.0 else Maff.fastTanh11(targetRangeDelta / 96.0)
        combat.maneuvers.applyForce(Forces.travel,     Potential.towards(unit, unit.agent.destinationNext()))
        combat.maneuvers.applyForce(Forces.threat,     Potential.hardAvoidThreatRange(unit, 96.0))
        combat.maneuvers.applyForce(Forces.regrouping, Potential.regroup(unit))
        combat.maneuvers.moveForcefully()
        combat.restrained = combat.target.forall(t => unit.pixelsToGetInRange(t) < unit.pixelsToGetInRangeFrom(t, unit.agent.destinationNext()))
        combat.restrained ||= unit.intent.terminus.exists(p => unit.pixelDistanceTravelling(p) < unit.pixelDistanceTravelling(unit.agent.destinationNext(), p))
      } else if (combat.techniqueIs(Scavenge)) {
        if ( ! PotshotBehavior.run(unit, combat)) {
          unit.agent.act("Snipe")
          val framesSafe      = unit.matchups.threatSoonest.map(_.framesToLaunchAttack(unit)).getOrElse(Forever())
          val framesToAttack  = combat.target.map(unit.framesToLaunchAttack).getOrElse(0)
          if (framesToAttack >= framesSafe || ! AttackIfReadyBehavior.run(unit, combat)) {
            unit.agent.act("Lurk")
            unit.agent.escalatePriority(TrafficPriorities.Bump) // Stronger than attackers, less than full retreaters
            combat.maneuvers.applyForce(Forces.target,   Potential.towardsTarget(unit))
            combat.maneuvers.applyForce(Forces.threat,   Potential.hardAvoidThreatRange(unit, Math.max(unit.topSpeed * framesToAttack, 64.0)))
            combat.maneuvers.applyForce(Forces.leaving,  Potential.towards(unit, unit.agent.safety) * combat.ground10)
            combat.maneuvers.moveForcefully()
          }
        }
      } else if (combat.distances.readyToApproachTarget || ! unit.matchups.wantsToVolley.contains(true) || KiteAway.exemptFromVolley(unit.totalHealth) || combat.shouldChase) {
        if ( ! KiteAwayBehavior.run(unit, combat) && ! ApproachBehavior.run(unit, combat)) {
          unit.agent.act("Engage")
          combat.maneuvers.charge()
        }
      } else {
        unit.agent.act("Reposition")
        val urgency01 = Math.max(0.0, (unit.cooldownMaxAirGround - unit.cooldownLeft).toDouble / (unit.cooldownMaxAirGround))
        // Haven't tried this out; might help with sliding into formation
        // engageFormation.flatMap(_(unit)).foreach(f => applyForce(Forces.travel, Potential.towards(unit, f)))
        combat.maneuvers.applyForce(Forces.target,   Potential.towardsTarget(unit) * urgency01)
        combat.maneuvers.applyForce(Forces.threat,   Potential.softAvoidThreatRange(unit))
        combat.maneuvers.applyForce(Forces.leaving,  Potential.towards(unit, unit.agent.safety) * combat.ground10 * (1 - urgency01))
        combat.maneuvers.moveForcefully()
      }
    }
    unit.unready
  }
}
