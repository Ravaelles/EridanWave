package Micro.Actions.Combat.Decisioning

import Debugging.Probe
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Actions.Combat.Decisioning.Formation.{FormationHold, OverwhelminglyStronger}
import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import Micro.Actions.Combat.Decisioning.Skirmish.HoldLineCreepBehavior
import Micro.Actions.Protoss.Zealot.ZealotLeash
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.ClusterCohesion
import Tactic.Squads.Decisions.ClusterPosture
import Tactic.Squads.Decisions.SuicideMission
import Utilities.Latch
import Utilities.?

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

  /**
   * Helpless prey, unit-level read for the engage gate and the formation
   * break below: workers-only contacts, or a three-to-one local skim
   * from the forecast the dressing already trusts.
   */
  private def preyDrive: Boolean = {
    val targets = unit.matchups.targets
    val workersOnly = targets.nonEmpty && targets.forall(_.unitClass.isWorker)
    val (usSkim, foeSkim) = unit.combatForecast
      .map(battle => (battle.us.skimStrengthTotal, battle.enemy.skimStrengthTotal))
      .getOrElse((0.0, 0.0))

    PreyDrive.shouldHunt(workersOnly, usSkim, foeSkim)
  }

  private def leashZealot(): Boolean = {
    if ( ! Protoss.Zealot(unit)) return false
    if (unit.agent.isScout || unit.agent.commit || With.yolo.active || SuicideMission.isActive(unit)) return false

    val anchor = Maff.minBy(With.units.ours.view.filter(u => u != unit && u.alive && Protoss.Dragoon(u)))(
      _.pixelDistanceCenter(unit.pixel))
    if (anchor.isEmpty) return false
    if ( ! ZealotLeash.shouldRejoin(anchor.get.pixelDistanceCenter(unit.pixel) * Maff.inv32)) return false

    unit.agent.act("ZealotLeash")
    unit.agent.forceTo(anchor.get.pixel)
    Commander.move(unit)
    true
  }

  private def rejoinCluster(): Boolean = {
    val squad = unit.squad.getOrElse(return false)
    if (unit.agent.isScout || unit.agent.commit || With.yolo.active || SuicideMission.isActive(unit)) return false
    if (unit.loaded) return false

    val centroid = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val contactTiles = 2.0
    if ( ! ClusterCohesion.shouldRejoin(
      isWarrior         = unit.unitClass.isWarrior,
      hasTarget         = unit.agent.toAttack.isDefined,
      retreating        = combat.shouldRetreat,
      coolingUnderFire  = unit.cooldownLeft > 0 && unit.matchups.threatsInRange.nonEmpty,
      inBattle          = unit.battle.isDefined,
      macroGame         = ! With.umsMode,
      hasLocalContact   = unit.matchups.enemies.exists(e => unit.pixelDistanceEdge(e) < unit.pixelRangeMax + contactTiles * 32.0),
      distTiles         = unit.pixelDistanceCenter(centroid) * Maff.inv32)) return false

    unit.agent.act("ClusterRejoin")
    unit.agent.forceTo(centroid)
    Commander.move(unit)
    true
  }

  private[Decisioning] def engage(): Boolean = {
    // Contain fire: a ready weapon shoots instead of dressing past the kill.
    if (ContainFireBehavior.run(unit)) return unit.unready

    // Cluster posture owns the duel (phase 4 of _COMBAT-REFACTOR.md):
    // Withdraw/Regroup walk the march, Fight/HoldLine duel. Explicit
    // all-ins bypass the cluster vote the way prey does: a suicide
    // mission or a yolo charge never walks home on a forecast.
    // Squads without a posture keep the old consensus gate untouched.
    val advance = unit.squad.flatMap(_.battlePosture) match {
      case Some(posture) => ClusterPosture.advances(posture) || SuicideMission.isActive(unit) || With.yolo.active
      case None          => combat.fightConsensus && ! unit.intent.marchHome
    }
    if ( ! advance && ! preyDrive) {
      combat.maneuvers.move()

      return true
    }

    // Zealot leash: a Zealot never operates past 5.5 tiles from our
    // Dragoons - a lone charge into their guns while ours fall back is a
    // donation, not pressure. Scouts, committed rushers, suicides and yolo
    // pushes keep their explicit orders; with no Dragoons mustered there
    // is nothing to leash to. Rejoins the line instead of dueling alone.
    if (leashZealot()) return true

    // Cluster cohesion: a warrior moving without a victim, neither
    // retreating nor cooling under fire, never drifts past fourteen tiles
    // from its cluster's centroid. Inside a battle that unit is lost -
    // walking at ghosts, stale slots or old orders - while the cluster
    // fights without it. Rejoins instead of hunting the map.
    if (rejoinCluster()) return true

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
    // Duel dispatch order is load-bearing too (first claim wins, same as
    // the shared chain above): breaking through beats scavenging, closing
    // beats repositioning, and the hold/approach/kite trio runs before the
    // raw charge. Never reorder without reading every leg - the push
    // claim, the volley gate and the chase override each read the same
    // target, and the first match owns the frame. No closing or charging
    // while retreating: the march goes home and the technique walks away,
    // so approaching the guns (or charging them outright) is exactly the
    // flap. KiteAway keeps its fighting withdrawal and TankHug keeps its
    // own tank logic - only the closing leg and the raw charge yield.
    if (combat.pushThrough) PushBehavior.run(unit, combat)
      else if (combat.techniqueIs(Scavenge)) ScavengeBehavior.run(unit, combat)
      else if (combat.distances.readyToApproachTarget || ! unit.matchups.wantsToVolley.contains(true) || KiteAway.exemptFromVolley(unit.totalHealth) || combat.shouldChase) {
        if ( ! TankHugBehavior.run(unit, combat) && ! KiteAwayBehavior.run(unit, combat) && ! combat.shouldRetreat && ! ApproachBehavior.run(unit, combat)) {
          unit.agent.act("Engage")
          combat.maneuvers.charge()
        }
      } else if ( ! SuicideEngageBehavior.run(unit) && ! IdleEngageBehavior.run(unit) && ! HoldLineCreepBehavior.run(unit, combat)) RepositionBehavior.run(unit, combat)
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
    combat.breakFormationToAttack ||= preyDrive
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
