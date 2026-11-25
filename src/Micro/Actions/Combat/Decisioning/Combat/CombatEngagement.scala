package Micro.Actions.Combat.Decisioning

import Debugging.DuelStarvation
import Debugging.HitsTaken
import Debugging.MotionMismatch
import Debugging.Probe
import Debugging.VolleyWaste
import Information.Battles.ArmyStrength
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Formation.{FormationHold, OverwhelminglyStronger}
import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Actions.UnitControl
import Micro.Actions.Combat.Decisioning.Kiting.{FacingAway, KiteAway}
import Micro.Agency.CommanderCore
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.ClusterPosture
import Tactic.Squads.Decisions.MotionVerdict
import Tactic.Squads.Decisions.RetreatVerdict
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

  /**
   * Contain fire (first claim): a ready weapon shoots instead of
   * dressing past the kill. Any hull the dispatch sends here - the
   * fight vote and the hittable target gate the shot below.
   */
  object ContainFireStage extends BehaviorStage("ContainFire", DispatchBand.High, Micro.Agency.UnitIntentionReason.Duel) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = true

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl =
      UnitControl.claimedIf(ContainFireBehavior.run(unit))
  }

  /**
   * Blind mop-up (second claim): a warrior with fight consensus but
   * no useful target walks the nearest remembered hittable enemy down -
   * with no live enemy in sight the timeout is the only alternative.
   * Gated on zero visible enemy warriors inside the behavior: any live
   * contact owns the frame through the duel and the formation, never a
   * ghost hunt. Unit contract beside the behavior; the map, the target
   * and the verdict gate below.
   */
  object HuntStage extends BehaviorStage("Hunt", DispatchBand.Medium, Micro.Agency.UnitIntentionReason.Duel) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = HuntBehavior.appliesToUnit(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl =
      // The dressed line owns outranged frames: hunting a ghost the line
      // holds against only walks it out of formation. Visible prey and
      // contacts belong to the duel and the hunt below untouched.
      if (ApproachEnemyFormation.eligible(unit, combat)) UnitControl.Declined
      else UnitControl.claimedIf(HuntBehavior.run(unit))
  }

  /**
   * No advance, no prey (third claim): walk the march instead of
   * dueling. Any hull - the gait is the march's, not a duel's.
   */
  object AdvanceGateStage extends BehaviorStage("AdvanceGate", DispatchBand.Low, Micro.Agency.UnitIntentionReason.March) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = true

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl = {
      Debugging.Probe.note("Gate", unit.id, s"advance=$advance prey=${preyDrive(unit)} consensus=${combat.fightConsensus} home=${unit.squadIntent.marchHome} commit=${unit.agent.commit}")
      // A loaded gun with something hittable never march-moves: the move
      // resets the firing solution on every consensus-off frame and the
      // volley never lands (BetaStar: VolleyHold/March flap till death).
      // The duel owns the frame instead - it holds or fires, never walks.
      // A blade with masonry hittable never march-moves either: its only
      // play against buildings is contact, so marching to a slot donates
      // while the hold below waits for a cooldown that never comes (PvZ:
      // VolleyHold/March flap at zero tiles). Retreating hulls keep the
      // march home; non-warriors keep walking.
      val aimingVolley = unit.unitClass.isWarrior && ! combat.shouldRetreat &&
        VolleyDiscipline.shouldVolley(retreating = false, unit.cooldownLeft, unit.matchups.targetsInRange.nonEmpty)
      val meleeStatic = unit.unitClass.melee && ! combat.shouldRetreat && Maff.minBy(unit.matchups.targetsInRange)(
        _.pixelDistanceEdge(unit)).exists(_.unitClass.isBuilding)
      if ( ! advance && ! preyDrive(unit) && ! aimingVolley && ! meleeStatic) { combat.maneuvers.move(); UnitControl.Claimed } else UnitControl.Declined
    }
  }

  /**
   * Zealot leash (fourth claim): a Zealot never operates past 5.5 tiles
   * from our Dragoons - a lone charge into their guns while ours fall
   * back is a donation, not pressure. Only blades; scouts, committed
   * rushers, suicides and yolo pushes keep their explicit orders, and
   * with no Dragoons mustered there is nothing to leash to.
   */
  object ZealotLeashStage extends BehaviorStage("ZealotLeash", DispatchBand.Medium, Micro.Agency.UnitIntentionReason.Formation) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = Protoss.Zealot(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl =
      UnitControl.claimedIf(ZealotLeashBehavior.run(unit))
  }

  /**
   * Cluster cohesion (fifth claim): a warrior moving without a victim,
   * neither retreating nor cooling under fire, never drifts past
   * fourteen tiles from its cluster's centroid. Warriors only; the
   * explicit orders, the battle state and the contact exemptions gate
   * below.
   */
  object ClusterRejoinStage extends BehaviorStage("ClusterRejoin", DispatchBand.Medium, Micro.Agency.UnitIntentionReason.Formation) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.LineArmy
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
      unit.unitClass.isWarrior && ! unit.loaded

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl =
      UnitControl.claimedIf(ClusterRejoinBehavior.run(unit, combat))
  }

  val stages: Vector[BehaviorStage] = Vector(
    ContainFireStage,
    HuntStage,
    AdvanceGateStage,
    ZealotLeashStage,
    ClusterRejoinStage)

  object PushDuelStage extends BehaviorStage("Push", DispatchBand.Medium, Micro.Agency.UnitIntentionReason.Duel) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    // Grounded hulls only: fliers dance instead of pushing, and the push
    // itself owns mobility - the gate never decides movement.
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = ! unit.flying

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl = {
      if ( ! combat.pushThrough) UnitControl.Declined else { PushBehavior.run(unit, combat); UnitControl.Claimed }
    }
  }

  object ScavengeDuelStage extends BehaviorStage("Scavenge", DispatchBand.Medium, Micro.Agency.UnitIntentionReason.Duel) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    // Any hull the technique assigns: the scavenger check lives in the
    // technique vote, the gate only runs what it is given.
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = true

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl = {
      if ( ! combat.techniqueIs(Scavenge)) UnitControl.Declined else { ScavengeBehavior.run(unit, combat); UnitControl.Claimed }
    }
  }

  /**
   * Formation (second claim): a warrior the duel sends to fight with
   * nothing hittable and no contact dresses the line instead of
   * attack-moving - slots when scattered, hold when slotted, creep
   * when dressed with a target past the gun. Fires before Close so the
   * hold owns outranged frames and the charge only sees duels.
   */
  object FormationDuelStage extends BehaviorStage("Formation", DispatchBand.Medium, Micro.Agency.UnitIntentionReason.Formation) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    // Any hull may form: the behavior gates the how below.
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = true

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl =
      UnitControl.claimedIf(ApproachEnemyFormation.run(unit, combat))
  }

  /**
   * Closing beats repositioning: the hold/kite duo runs before
   * the raw charge. No closing or charging while retreating: the march
   * goes home and the technique walks away, so approaching the guns (or
   * charging them outright) is exactly the flap. KiteAway keeps its
   * fighting withdrawal and TankHug keeps its own tank logic - only the
   * raw charge yields. A taken branch still claims
   * the frame when its inner legs decline, so Reposition never runs
   * behind it. Any hull may close: the duo gates the how below.
   */
  object CloseDuelStage extends BehaviorStage("Close", DispatchBand.Medium, Micro.Agency.UnitIntentionReason.Duel) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    // Any hull may close: the hold/kite duo gates the how
    // below, including the immobile hold it never walks into.
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = true

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl = {
      // Lone blade chase (owner order, 1Drag_v_1Zeal): with no guns nearby
      // the duel is free, so a chased hull always enters Close -
      // otherwise the volley branch above declines healthy hulls while the
      // dance stands down for a kite that never comes, and Reposition
      // slides into contact instead.
      val gunsNear = unit.matchups.threats.exists(t =>
        t.likelyStillThere && t.canAttack(unit) && ! t.unitClass.melee &&
        unit.pixelDistanceEdge(t) * Maff.inv32 <= KiteAway.chaseTiles)
      val meleeChase = unit.matchups.threatsInPixels(KiteAway.chaseTiles * 32.0).exists(t =>
        t.likelyStillThere && t.unitClass.melee &&
        ! FacingAway.dismissMelee(isMelee = true, FacingAway.showingBack(t.angleRadians, t.pixel.radiansTo(unit.pixel))))
      // A volley nobody wants still needs reach: without contact, a
      // hittable gun or a seen enemy inside formation range the duel
      // would charge across the map under auto-acquire - the march and
      // the formation own those frames instead.
      val noVolley = ! unit.matchups.wantsToVolley.contains(true) && KiteAway.duelInReach(
        hasContact = unit.matchups.threatsInRange.nonEmpty,
        hasHittable = unit.matchups.targetsInRange.nonEmpty,
        seenNear = ApproachEnemyFormation.seenNearFoe(unit))
      val outer = combat.distances.readyToApproachTarget || noVolley || KiteAway.exemptFromVolley(unit.totalHealth) || combat.shouldChase || KiteAway.loneBladeDuel(gunsNear, meleeChase, unit.unitClass.melee)
      if ( ! outer) UnitControl.Declined
      else {
        // Tanks are never closed: the hold stands off siege and unsieged
        // guns alike instead of walking into unanswered fire. TankHug
        // keeps its point-blank exception above, the kite keeps its
        // fighting withdrawal - only the raw charge yields to the hold.
        // The act logs the veto, so the trace shows the hold instead of
        // a silent march-in. The old Approach leg is gone, merged into
        // the Formation governor: one line, one advancer.
        if ( ! TankHugBehavior.run(unit, combat) && ! KiteAwayBehavior.run(unit, combat) && ! TankHoldBehavior.run(unit, combat)) {
          // Retreat veto never silent-claims: a retreating hull with no
          // leg acting hands the frame to Reposition (which always signs)
          // instead of standing idle under a stale stage label.
          if (combat.shouldRetreat) UnitControl.Declined
          else {
            unit.agent.act("Engage")
            combat.maneuvers.charge()
            UnitControl.Claimed
          }
        }
        else UnitControl.Claimed
      }
    }
  }

  /**
   * Terminal: claim value moot, nothing follows. Any hull the duel
   * leaves behind repositions through it. The old HoldLine advancer is
   * gone, merged into the Formation governor: one line, one stepper.
   */
  object RepositionDuelStage extends BehaviorStage("Reposition", DispatchBand.Low, Micro.Agency.UnitIntentionReason.Duel) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    // Any hull the duel leaves behind repositions through here: the
    // terminal claims for everyone, the maneuver handles the rest.
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = true

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl = {
      if ( ! SuicideEngageBehavior.run(unit) && ! IdleEngageBehavior.run(unit)) RepositionBehavior.run(unit, combat)
      UnitControl.Claimed
    }
  }

  val duelStages: Vector[BehaviorStage] = Vector(
    PushDuelStage,
    ScavengeDuelStage,
    FormationDuelStage,
    CloseDuelStage,
    RepositionDuelStage)

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
}

class CombatEngagement(combat: Combat, unit: FriendlyUnitInfo) {

  /**
   * Pursuit latch (auto-registered, reset per game with the rest): once the
   * line breaks for a target past the pursuit horizon it stays broken until
   * contact or incoming fire. Without the memory the cap edge itself
   * becomes the new standoff - the line holds at twelve tiles instead of
   * twenty-five and the dummy still never dies. A live enemy pressing the
   * pack is never pursued: let them walk into the dressed line instead of
   * meeting them halfway - the vanguard takes its slot and the arc
   * receives the charge together. Pressing reads pack-relative (toward
   * the centroid), never per-hull: chargers converge on the pack, so a
   * per-hull read calls them fleeing from the back line. Passive and
   * fleeing targets keep the old walk-down.
   */
  private val pursuit = new Latch

  private def pursuing(): Boolean = {
    val packCentroid = unit.squad.map(squad =>
      ?(unit.flying, squad.centroidAir, squad.centroidGround))
    // Nobody gets left behind: the line walks a dummy down together, so
    // the pursuit waits until every warrior holds a slot - a straggler
    // chasing alone is the snake, and its slotless hold already covers
    // its feet until the arc has room.
    val unplacedWarriors = unit.squad.map(_.units.count(u =>
      u.alive && u.unitClass.isWarrior && u.formationEngage.isEmpty)).getOrElse(0)
    pursuit(
      unit.id,
      fires = combat.target.exists(t =>
        unit.pixelsToGetInRange(t) > FormationHold.holdPursuitTiles * 32.0
        && unplacedWarriors == 0
        && ! (t.visible && packCentroid.exists(t.speedApproaching(_) > 0))),
      releases = combat.target.forall(t =>
        unit.pixelsToGetInRange(t) <= FormationHold.holdReleaseTiles * 32.0)
        || unit.matchups.threatsInRange.exists(_.cooldownLeft > 0)
        || unit.matchups.engagedUpon)
  }

  private[Decisioning] def engage(): Boolean = {
    // Cluster posture owns the duel (phase 4 of _COMBAT-REFACTOR.md):
    // Withdraw/Regroup walk the march, Fight/HoldLine duel. Explicit
    // all-ins bypass the cluster vote the way prey does: a suicide
    // mission or a yolo charge never walks home on a forecast.
    // Squads without a posture read the latched feet-owner (MotionVerdict)
    // instead of the raw consensus: a held duel owns the frame through
    // the sim seesaw, exactly like the dispatch gate above reads the
    // held retreat. One latched advance shared by both gates, so no
    // threshold inside either can flap the march against the duel.
    // A unit the march sent home does not duel on the way out even
    // when the cluster advances: its destination is home, the
    // rearguard and the Retreat maneuver above already spoke for it,
    // and dispatching it back into the fight is exactly the mixed
    // frame the retreat discipline exists to kill.
    // Committed UMS duelists advance while no retreat is held: the sim
    // forecasts trades, but a losing trade the unit can kite is still a
    // duel - without advance the technique never runs and the march
    // walks losing duels out of the map instead of kiting them. Same
    // held verdict the dispatch reads, so both gates agree. Retreating
    // units keep marching home.
    val umsDuelistAdvances = With.umsMode && unit.agent.commit &&
      ! (RetreatVerdict.heldArmy(With.frame) && ! ClusterPosture.overrulesRetreat(
        unit.squad.flatMap(_.battlePosture),
        ArmyStrength.score(
          With.battles.globalAttack.us.skimStrengthUnits,
          With.battles.globalAttack.enemy.skimStrengthUnits)))
    val advance = (unit.squad.flatMap(_.battlePosture) match {
      case Some(posture) => (ClusterPosture.advances(posture) || SuicideMission.held(unit) || With.yolo.active) && ! unit.squadIntent.marchHome
      case None =>
        // Latched feet-owner, not the raw consensus (owner order): a held
        // duel rides through the sim seesaw instead of flapping the march
        // and the duel every other frame. Withdraw never duels on the way
        // out; Dress and the open march keep the old consensus gate.
        EngageAdvance.posturelessAdvance(
          MotionVerdict.held(unit.id, With.frame),
          combat.fightConsensus,
          unit.squadIntent.marchHome)
    }) || umsDuelistAdvances
    // Arbitration window (live since Step 3): the stages stamp their
    // reason in the shared template and post UnitIntention bids; the
    // resolve issues the winner once - highest rank wins, first posted
    // wins ties. Loop control stays first-claims (later bodies carry
    // load-bearing side effects).
    unit.agent.capturingBids = true
    unit.agent.bidReason = None
    var i = 0
    var claimed = false
    while (i < EngagePipeline.stages.length && ! claimed) {
      claimed = EngagePipeline.stages(i).shouldRun(unit, combat, advance).claimed
      i += 1
    }
    if (claimed) {
      CommanderCore.resolveBids(unit)

      return true
    }

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
      dispatchEngage(advance)
      CommanderCore.resolveBids(unit)
      // L1 rivalry tripwire: the duel legs issued while the march holds
      // Withdraw - the technique fights a walk-home. Release noise never
      // reaches the ledger threshold; sustained streaks report at game end.
      if (unit.unready && MotionVerdict.held(unit.id, With.frame) == MotionVerdict.Withdraw) {
        MotionMismatch.bumpDuel(unit.id, unit.unitClass.toString, With.frame)
      }
      // Wasted-volley ledger, end of frame: a loaded gun with a hittable
      // target that ends the frame moving judges the kite - it must fall,
      // kills must not. A loaded gun standing still and silent past a
      // full period with an enemy at the edge judges the plant the same
      // way. Read after the dispatch, never before it: the
      // pre-dispatch order belongs to last frame, so every volley fired
      // after a step counted as waste even though the shot landed. Only
      // ready frames are judged - a slept frame owns no orders, and its
      // starvation is counted beside it instead. Near means seen: fogged
      // ghosts cannot be shot, so holding at them counts nothing.
      VolleyWaste.bump(unit.id, unit.unitClass.toString, unit.cooldownLeft, unit.matchups.targetsInRange.nonEmpty, unit.order, unit.unitClass.isWarrior,
        frame = With.frame, staleFrames = With.latency.latencyFrames + 1,
        hasHittableNear = unit.matchups.targets.exists(t => t.visible && unit.pixelsToGetInRange(t) <= VolleyWaste.stuckMarginPixels),
        standingStill = unit.speed < 1.0,
        framesSinceAttack = With.framesSince(unit.lastFrameStartingAttack),
        periodFrames = unit.cooldownMaxAirGround,
        latencyFrames = With.latency.latencyFrames)
      // Hits-taken ledger, same frame: every hp drop counts as a landed
      // hit, no matter the guns. The kite answers for it at game end.
      if (unit.unitClass.isWarrior) HitsTaken.bump(unit.id, unit.unitClass.toString, unit.totalHealth)
    } else {
      // Unready on entry: no bids were posted (the seam drops slept
      // frames), the resolve only clears the window for the next unit.
      CommanderCore.resolveBids(unit)
      // Tripwire for starved duels: the dispatch voted for the duel but
      // the legs never ran - a sleeping march or a legacy brain issuing
      // orders ahead of the technique does exactly this. Counted, not
      // probed: one integer per frame, read once at game end.
      DuelStarvation.bump(unit.id, unit.unitClass.toString)
    }
    unit.unready
  }

  private def dispatchEngage(advance: Boolean): Unit = {
    var i = 0
    var claimed = false
    while (i < EngagePipeline.duelStages.length && ! claimed) {
      claimed = EngagePipeline.duelStages(i).shouldRun(unit, combat, advance).claimed
      i += 1
    }
  }

  // Formation resolution: break for the attack once no formation exists,
  // the target is approached, close, or yolo overrules. Past the pursuit
  // horizon the formation is a freeze, not a line: no living enemy holds
  // still that far out while staying a threat, so break and walk it down
  // instead of dressing for a fight that never starts (UMS duels held a
  // twenty-tile standoff against passive hulls until the timeout).
  // A slotless straggler holds while its formed pack stands calm
  // (owner order, 8Zeal_v_LingUp): with no pixel of its own, breaking
  // means walking at the foe alone - the snake. The march owns its feet
  // (rally home on ladder, hold on UMS) until contact or the pack fight,
  // then it joins like everyone else. Formless packs fight as before.
  private def resolveBreakFormation(): Unit = {
    combat.breakFormationToAttack  = unit.formationEngage.isEmpty && ! slotlessHolds()
    combat.breakFormationToAttack ||= pursuing()
    combat.breakFormationToAttack ||= combat.target.exists(targ =>
      // Maintain formation if we're not ready to attack yet
      combat.distances.readyToApproachTarget
      // Maintain formation until arriving at a fight or the destinatation.
      // Melee arrives with the pack fight, not at sight or with a merely
      // assigned battle: a seen target eight tiles out is a slot-walk and
      // receive, never a charge - the line broke for screens and met the
      // main force piecemeal. Solo contact still breaks below, ranged
      // keeps the sight approach.
      && ((unit.battle.isDefined && ! unit.unitClass.melee)
        || (! unit.unitClass.melee && (unit.pixelDistanceEdge(targ) <= unit.sightPixels || (unit.pixelDistanceEdge(targ) <= 256 && targ.visible)))
        || (unit.unitClass.melee && combat.squadEngagedUpon))
      // Maintain formation while flanking
      && ! unit.formationEngage.exists(_.flanking)
      // Maintain formation until battle breaks out
      && ( ! combat.formationHelpsEngage || combat.squadEngagedUpon))
    combat.breakFormationToAttack ||= ! unit.formationEngage.exists(_.flanking) && combat.target.exists(unit.pixelsToGetInRange(_) < Math.max(0, 128 * unit.confidence11))
    combat.breakFormationToAttack ||= With.yolo.active
    combat.breakFormationToAttack ||= breakForPrey()
  }

  /**
   * Helpless-prey break (owner order, 8Zeal_v_LingUp): workers-only
   * contacts break at any range (nothing shoots back), but a 3:1 skim
   * breaks only at contact range - a screen at 8-12 tiles is bait for
   * the unseen main force, not mop-up, and the audit watched the line
   * dissolve for it in the first seconds. The dressed line lets the
   * screen walk in instead of trickling out to meet it.
   */
  private def breakForPrey(): Boolean = {
    if ( ! EngagePipeline.preyDrive(unit)) return false

    val targets = unit.matchups.targets
    val workersOnly = targets.nonEmpty && targets.forall(_.unitClass.isWorker)

    workersOnly || combat.target.exists(t =>
      unit.pixelsToGetInRange(t) < Math.max(0, 128 * unit.confidence11))
  }

  /**
   * Slotless hold (owner order, 8Zeal_v_LingUp): a warrior with no
   * placement in a standing formation holds instead of breaking while
   * its pack stands calm - no pixel means no slot to walk, and breaking
   * means a lone walk at the foe. Contact and pack fights still break.
   */
  private def slotlessHolds(): Boolean =
    unit.unitClass.isWarrior &&
      unit.formationEngage.isEmpty &&
      unit.squad.exists(_.formationEngage.isDefined) &&
      unit.matchups.targetsInRange.isEmpty &&
      unit.matchups.threatsInRange.isEmpty &&
      ! combat.squadEngagedUpon

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
