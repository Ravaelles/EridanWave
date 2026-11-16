package Micro.Actions.Combat.Decisioning

import Debugging.Probe
import Lifecycle.With
import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Actions.Combat.Decisioning.Formation.{FormationHold, OverwhelminglyStronger}
import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Latch

class CombatEngagement(combat: Combat, unit: FriendlyUnitInfo) {

  /**
   * Pursuit latch (auto-registered, reset per game with the rest): once the
   * line breaks for a target past the pursuit horizon it stays broken until
   * contact or incoming fire. Without the memory the cap edge itself
   * becomes the new standoff - the line holds at twelve tiles instead of
   * twenty-five and the dummy still never dies.
   */
  private val pursuit = new Latch

  private def pursuing(): Boolean =
    pursuit(
      unit.id,
      fires = combat.target.exists(t =>
        unit.pixelsToGetInRange(t) > FormationHold.holdPursuitTiles * 32.0),
      releases = combat.target.forall(t =>
        unit.pixelsToGetInRange(t) <= FormationHold.holdReleaseTiles * 32.0)
        || unit.matchups.threatsInRange.exists(_.cooldownLeft > 0)
        || unit.matchups.engagedUpon)

  private[Decisioning] def engage(): Boolean = {
    // Crucial air victims outrank everything: a grounded anti-air warrior
    // shoots Scourge, Carriers, Vessels, Battlecruisers and Wraiths in
    // range first - even while retreating - then carries on as ordered.
    if (CrucialAirSnipeBehavior.run(unit)) return unit.unready

    // Contain fire: a ready weapon shoots instead of dressing past the kill.
    if (ContainFireBehavior.run(unit)) return unit.unready

    // No consensus: the squad is leaving, so walk its march instead of
    // dueling. Lone counterattacks while regrouping feed the enemy piecemeal;
    // the contain roster and mode latch still hold, so the squad reforms.
    // March-home counts too: the verdict rides the frame-fresh intention,
    // so a unit the march sent home never re-engages in this phase even
    // when the consensus changed hands between the phases.
    if ( ! combat.fightConsensus || unit.intent.marchHome) {
      combat.maneuvers.move()

      return true
    }

    // No target, or a target the line would only pose at: hunt the
    // nearest known enemy instead of dressing until the timeout.
    if (HuntBehavior.run(unit)) return true

    if (unit.ready) {
      resolveBreakFormation()
      // A pursuit in progress is never re-dressed: the hold would re-arm
      // at the cap edge and the march would stall twelve tiles out.
      if ( ! pursuing()) applyFormationHold()
      if (With.configuration.formationAudit) {
        Debugging.FormationAudit.noteBreakState(
          With.frame,
          unit.id,
          unit.unitClass.toString,
          combat.breakFormationToAttack,
          unit.formationEngage.isDefined,
          combat.target.map(t => unit.pixelsToGetInRange(t) / 32.0),
          combat.target.map(_.unitClass.toString))
      }

      if ( ! combat.breakFormationToAttack) {
        unit.agent.perch.clear()
      }
      dispatchEngage()
    }
    unit.unready
  }

  private def dispatchEngage(): Unit = {
    if (combat.pushThrough) PushBehavior.run(unit, combat)
      else if (combat.techniqueIs(Scavenge)) ScavengeBehavior.run(unit, combat)
      else if (combat.distances.readyToApproachTarget || ! unit.matchups.wantsToVolley.contains(true) || KiteAway.exemptFromVolley(unit.totalHealth) || combat.shouldChase) {
        if ( ! TankHugBehavior.run(unit, combat) && ! KiteAwayBehavior.run(unit, combat) && ! ApproachBehavior.run(unit, combat)) {
          unit.agent.act("Engage")
          combat.maneuvers.charge()
        }
      } else if ( ! SuicideEngageBehavior.run(unit) && ! IdleEngageBehavior.run(unit)) RepositionBehavior.run(unit, combat)
  }

  // Formation resolution: break for the attack once no formation exists,
  // the target is approached, close, or yolo overrules. Past the pursuit
  // horizon the formation is a freeze, not a line: no living enemy holds
  // still that far out while staying a threat, so break and walk it down
  // instead of dressing for a fight that never starts (UMS duels held a
  // twenty-tile standoff against passive hulls until the timeout).
  private def resolveBreakFormation(): Unit = {
    combat.breakFormationToAttack  = unit.formationEngage.isEmpty
    combat.breakFormationToAttack ||= pursuing()
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
  // shoots. The hold also releases once the fight is on for this unit: an
  // enemy in range whose weapon is cooling down (it opened fire), or this
  // unit itself inside an enemy's range. A squad mate shooting elsewhere
  // must not turn the whole line into a charge. Melee must still close,
  // flankers must still circle, and an explicit yolo overrules.
  private def applyFormationHold(): Unit = {
    lazy val hold = combat.target.exists(t =>
      FormationHold.shouldHoldLine(
        tilesBeyondRange  = unit.pixelsToGetInRange(t) / 32.0,
        melee             = unit.unitClass.melee,
        hasFormation      = unit.formationEngage.isDefined,
        flanking          = unit.formationEngage.exists(_.flanking),
        unitEngaged       = unit.matchups.engagedUpon,
        enemyFired        = unit.matchups.threatsInRange.exists(_.cooldownLeft > 0),
        targetHoldable    = t.unitClass.isWarrior && t.unitClass.canAttack && (! t.unitClass.isBuilding || t.unitClass.isCombatBuilding)))
    combat.target.foreach(t => Probe.note("Hold", unit.id, f"form=${unit.formationEngage.isDefined} flank=${unit.formationEngage.exists(_.flanking)} me=${unit.matchups.engagedUpon} fired=${unit.matchups.threatsInRange.exists(_.cooldownLeft > 0)} beyond=${unit.pixelsToGetInRange(t) / 32.0}%.1f hold=$hold target=${t.id}"))
    if ( ! OverwhelminglyStronger.skipFormation(unit) && ! With.yolo.active && hold) {
      combat.breakFormationToAttack = false
    }
  }
}
