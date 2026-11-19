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
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.ClusterCohesion
import Tactic.Squads.Decisions.ClusterPosture
import Tactic.Squads.Decisions.SuicideMission
import Utilities.DispatchBand
import Utilities.Latch
import Utilities.?


/**
 * Engage pipelines (one explicit place, first claim wins): the duel
 * dispatch and its engage gate share this registry. Reorder by moving a
 * line, disable by commenting one out; bands say which arbitration each
 * stage fights in. Pinned by EngageDispatchOrderSuite. Every expression
 * below is the old ladder line verbatim - the lists change the shape,
 * never the behavior.
 */
private[Decisioning] object EngagePipeline {

  case class Stage(name: String, band: DispatchBand, run: (FriendlyUnitInfo, Combat, Boolean) => Option[Boolean])

  val stages: Vector[Stage] = Vector(
    // Contain fire: a ready weapon shoots instead of dressing past the kill.
    Stage("ContainFire", DispatchBand.High, (unit, _, _) =>
      if (ContainFireBehavior.run(unit)) Some(unit.unready) else None),
    // No target, or a target the line would only pose at: hunt the
    // nearest known enemy instead of dressing until the timeout. Runs
    // ahead of the advance gate on purpose: the gate walks the march
    // when there is nothing to duel, but a remembered contact is
    // something to duel, and on UMS maps hunting is always right -
    // otherwise the line dresses at stale slots while the last enemy
    // walks away and the map times out. Off UMS the hunt is inert
    // (no game access beyond the umsMode flag), so macro order is
    // untouched.
    Stage("Hunt", DispatchBand.Medium, (unit, _, _) =>
      if (HuntBehavior.run(unit)) Some(true) else None),
    // No advance, no prey: walk the march instead of dueling.
    Stage("AdvanceGate", DispatchBand.Low, (unit, combat, advance) =>
      if ( ! advance && ! preyDrive(unit)) { combat.maneuvers.move(); Some(true) } else None),
    // Zealot leash: a Zealot never operates past 5.5 tiles from our
    // Dragoons - a lone charge into their guns while ours fall back is a
    // donation, not pressure. Scouts, committed rushers, suicides and yolo
    // pushes keep their explicit orders; with no Dragoons mustered there
    // is nothing to leash to. Rejoins the line instead of dueling alone.
    Stage("ZealotLeash", DispatchBand.Medium, (unit, combat, _) =>
      if (leashZealot(unit, combat)) Some(true) else None),
    // Zealot ball: with no Dragoons to leash to, blades facing a melee
    // swarm glue to the nearest blade instead of dueling alone - lone
    // blades kiting in opposite directions is how packs get picked
    // apart. Scouts, commits, suicides and yolo keep their orders.
    Stage("ZealotBall", DispatchBand.Medium, (unit, combat, _) =>
      if (ballZealots(unit, combat)) Some(true) else None),
    // Cluster cohesion: a warrior moving without a victim, neither
    // retreating nor cooling under fire, never drifts past fourteen tiles
    // from its cluster's centroid. Inside a battle that unit is lost -
    // walking at ghosts, stale slots or old orders - while the cluster
    // fights without it. Rejoins instead of hunting the map.
    Stage("ClusterRejoin", DispatchBand.Medium, (unit, combat, _) =>
      if (rejoinCluster(unit, combat)) Some(true) else None))

  case class DuelStage(name: String, band: DispatchBand, run: (FriendlyUnitInfo, Combat) => Boolean)

  val duelStages: Vector[DuelStage] = Vector(
    // Breaking through beats scavenging: the push owns the frame.
    DuelStage("Push", DispatchBand.Medium, (unit, combat) =>
      if ( ! combat.pushThrough) false else { PushBehavior.run(unit, combat); true }),
    DuelStage("Scavenge", DispatchBand.Medium, (unit, combat) =>
      if ( ! combat.techniqueIs(Scavenge)) false else { ScavengeBehavior.run(unit, combat); true }),
    // Closing beats repositioning: the hold/approach/kite trio runs before
    // the raw charge. No closing or charging while retreating: the march
    // goes home and the technique walks away, so approaching the guns (or
    // charging them outright) is exactly the flap. KiteAway keeps its
    // fighting withdrawal and TankHug keeps its own tank logic - only the
    // closing leg and the raw charge yield. A taken branch still claims
    // the frame when its inner legs decline, so Reposition never runs
    // behind it.
    DuelStage("Close", DispatchBand.Medium, (unit, combat) => {
      val outer = combat.distances.readyToApproachTarget || ! unit.matchups.wantsToVolley.contains(true) || KiteAway.exemptFromVolley(unit.totalHealth) || combat.shouldChase
      if ( ! outer) false
      else {
        // Tanks are never closed: the hold stands off siege and unsieged
        // guns alike instead of walking into unanswered fire. TankHug
        // keeps its point-blank exception above, the kite keeps its
        // fighting withdrawal - only the closing leg and the raw charge
        // yield to the hold. The act logs the veto, so the trace shows
        // the hold instead of a silent march-in.
        if ( ! TankHugBehavior.run(unit, combat) && ! KiteAwayBehavior.run(unit, combat) && ! TankHoldBehavior.run(unit, combat) && ! combat.shouldRetreat && ! ApproachBehavior.run(unit, combat)) {
          unit.agent.act("Engage")
          combat.maneuvers.charge()
        }
        true
      }
    }),
    // Terminal: claim value moot, nothing follows.
    DuelStage("Reposition", DispatchBand.Low, (unit, combat) => {
      if ( ! SuicideEngageBehavior.run(unit) && ! IdleEngageBehavior.run(unit) && ! HoldLineCreepBehavior.run(unit, combat)) RepositionBehavior.run(unit, combat)
      true
    }))

  def order: Seq[(String, DispatchBand)] =
    stages.map(s => (s.name, s.band))

  def duelOrder: Seq[(String, DispatchBand)] =
    duelStages.map(s => (s.name, s.band))

  /**
   * Helpless prey, unit-level read for the engage gate and the formation
   * break below: workers-only contacts, or a three-to-one local skim
   * from the forecast the dressing already trusts.
   */
  private[Decisioning] def preyDrive(unit: FriendlyUnitInfo): Boolean = {
    val targets = unit.matchups.targets
    val workersOnly = targets.nonEmpty && targets.forall(_.unitClass.isWorker)
    val (usSkim, foeSkim) = unit.combatForecast
      .map(battle => (battle.us.skimStrengthTotal, battle.enemy.skimStrengthTotal))
      .getOrElse((0.0, 0.0))

    PreyDrive.shouldHunt(workersOnly, usSkim, foeSkim)
  }

  /**
   * Leash latch (auto-registered, reset per game): the slot leg and the
   * leash leg share one actuator, so a raw 5.5-tile boundary trades the
   * Zealot every few frames instead of committing to either leg.
   */
  private val zealotLeash = new Latch

  private def leashZealot(unit: FriendlyUnitInfo, combat: Combat): Boolean = {    if ( ! Protoss.Zealot(unit)) return false
    if (unit.agent.isScout || unit.agent.commit || With.yolo.active || SuicideMission.isActive(unit)) return false

    val anchor = Maff.minBy(With.units.ours.view.filter(u => u != unit && u.alive && Protoss.Dragoon(u)))(
      _.pixelDistanceCenter(unit.pixel))
    if (anchor.isEmpty) return false

    val distTiles = anchor.get.pixelDistanceCenter(unit.pixel) * Maff.inv32
    // A blade on a sieged tank stays on it: the leash would trade a
    // free point-blank kill for a walk home, the same no-reason retreat
    // the technique floor already forbids.
    if (Maff.minBy(unit.matchups.enemies.view.filter(e =>
      e.alive && Terran.SiegeTankSieged(e)))(
      _.pixelDistanceEdge(unit)).exists(t => TankHug.holdsTank(unit.pixelDistanceEdge(t) * Maff.inv32, isMelee = true))) return false
    if ( ! zealotLeash(
      unit.id,
      fires = ZealotLeash.shouldRejoin(distTiles),
      releases = distTiles < ZealotLeash.leashReleaseTiles)) return false

    unit.agent.act("ZealotLeash")
    unit.agent.forceTo(anchor.get.pixel)
    Commander.move(unit)
    true
  }

  /**
   * Ball glue (thin seam over the pure rule): with no Dragoons mustered
   * the leash above has nothing to hold, so blades facing a melee swarm
   * glue to the nearest blade instead of dueling alone. Scouts, commits,
   * suicides and yolo keep their explicit orders; the duel still picks
   * targets, the ball only votes movement, so the line swings together.
   */
  private def ballZealots(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! Protoss.Zealot(unit)) return false
    if (unit.agent.isScout || unit.agent.commit || With.yolo.active || SuicideMission.isActive(unit)) return false

    val mate = Maff.minBy(With.units.ours.view.filter(u => u != unit && u.alive && Protoss.Zealot(u)))(
      _.pixelDistanceCenter(unit.pixel))
    if (mate.isEmpty) return false

    val marginPixels = KiteAway.enemyMarginTiles * 32.0
    val meleeFoes = unit.matchups.threatsInPixels(marginPixels).count(_.unitClass.melee)
    if ( ! ZealotLeash.shouldBall(mate.get.pixelDistanceCenter(unit.pixel) * Maff.inv32, meleeFoes)) return false

    unit.agent.act("ZealotBall")
    unit.agent.forceTo(mate.get.pixel)
    Commander.move(unit)
    true
  }

  private def rejoinCluster(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    val squad = unit.squad.getOrElse(return false)
    if (unit.agent.isScout || unit.agent.commit || With.yolo.active || SuicideMission.isActive(unit)) return false
    if (unit.loaded) return false
    // A committed cast walk owns the frame: yanking a Templar with a
    // picked victim back to the centroid every other frame means the
    // four-frame stand never happens and a full battery circles forever.
    // The intent expires on its own if the walk goes stale, and the
    // rejoin resumes - no stuck casters.
    if (unit.agent.spellIntent.isDefined) return false

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
}

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
    // Cluster posture owns the duel (phase 4 of _COMBAT-REFACTOR.md):
    // Withdraw/Regroup walk the march, Fight/HoldLine duel. Explicit
    // all-ins bypass the cluster vote the way prey does: a suicide
    // mission or a yolo charge never walks home on a forecast.
    // Squads without a posture keep the old consensus gate untouched.
    // A unit the march sent home does not duel on the way out even
    // when the cluster advances: its destination is home, the
    // rearguard and the Retreat maneuver above already spoke for it,
    // and dispatching it back into the fight is exactly the mixed
    // frame the retreat discipline exists to kill.
    val advance = unit.squad.flatMap(_.battlePosture) match {
      case Some(posture) => (ClusterPosture.advances(posture) || SuicideMission.isActive(unit) || With.yolo.active) && ! unit.intent.marchHome
      case None          => combat.fightConsensus && ! unit.intent.marchHome
    }
    var i = 0
    var claimed: Option[Boolean] = None
    while (i < EngagePipeline.stages.length && claimed.isEmpty) {
      claimed = EngagePipeline.stages(i).run(unit, combat, advance)
      i += 1
    }
    if (claimed.isDefined) return claimed.get

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
    var i = 0
    var claimed = false
    while (i < EngagePipeline.duelStages.length && ! claimed) {
      claimed = EngagePipeline.duelStages(i).run(unit, combat)
      i += 1
    }
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
    combat.breakFormationToAttack ||= EngagePipeline.preyDrive(unit)
  }

  // Hold the line while a ranged armed combat target is well beyond our
  // firing range (tiles = pixels / 32, as in Atlantis): keep formation
  // slots and let the enemy come to us instead of chasing piecemeal.
  // Only what can shoot back on the ground holds the line (isHoldableTarget):
  // air-only statics like Turrets never pin a formation - they cannot
  // answer a Dragoon, so the line marches them down instead of posing.
  // Buildings that shoot ground (Sunkens, Cannons) still hold it.
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
        targetHoldable    = FormationHold.isHoldableTarget(
          t.unitClass.isWarrior,
          t.unitClass.attacksGround,
          t.unitClass.isBuilding,
          t.unitClass.isCombatBuilding)))
    combat.target.foreach(t => Probe.note("Hold", unit.id, f"form=${unit.formationEngage.isDefined} flank=${unit.formationEngage.exists(_.flanking)} me=${unit.matchups.engagedUpon} fired=${unit.matchups.threatsInRange.exists(_.cooldownLeft > 0)} beyond=${unit.pixelsToGetInRange(t) / 32.0}%.1f hold=$hold target=${t.id}"))
    if ( ! OverwhelminglyStronger.skipFormation(unit) && ! With.yolo.active && hold) {
      combat.breakFormationToAttack = false
    }
  }
}
