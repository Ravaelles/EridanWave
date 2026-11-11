package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Actions.Combat.Decisioning.Formation.{FormationHold, OverwhelminglyStronger}
import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

class CombatEngagement(combat: Combat, unit: FriendlyUnitInfo) {

  private[Decisioning] def engage(): Boolean = {
    if (unit.ready) {
      resolveBreakFormation()
      applyFormationHold()

      if ( ! combat.breakFormationToAttack) {
        unit.agent.perch.clear()
      }
      dispatchEngage()
    }
    unit.unready
  }

  private def dispatchEngage(): Unit = {
    if (combat.pushThrough) PushBehavior.run(unit, combat)
      else if (combat.assembling && combat.group.groupUnits.size > 1) AssembleBehavior.run(unit, combat)
      else if (combat.techniqueIs(Scavenge)) ScavengeBehavior.run(unit, combat)
      else if (combat.distances.readyToApproachTarget || ! unit.matchups.wantsToVolley.contains(true) || KiteAway.exemptFromVolley(unit.totalHealth) || combat.shouldChase) {
        if ( ! KiteAwayBehavior.run(unit, combat) && ! ApproachBehavior.run(unit, combat)) {
          unit.agent.act("Engage")
          combat.maneuvers.charge()
        }
      } else RepositionBehavior.run(unit, combat)
  }

  // Formation resolution: break for the attack once no formation exists,
  // the target is approached, close, or yolo overrules.
  private def resolveBreakFormation(): Unit = {
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
  }

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
  private def applyFormationHold(): Unit = {
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
  }
}
