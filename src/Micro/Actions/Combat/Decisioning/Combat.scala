package Micro.Actions.Combat.Decisioning

import Debugging.Probe
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Actions.Combat.Maneuvering.AvoidDarkTemplar
import Micro.Actions.Combat.Maneuvering.AvoidLurkers
import Micro.Actions.Combat.Maneuvering.Retreat
import Micro.Actions.Combat.Maneuvering.ArtilleryKeepOff
import Micro.Actions.Combat.Tactics.Brawl
import Micro.Agency.Commander
import Micro.Coordination.Pushing.TrafficPriorities
import Tactic.Squads.Decisions.ClusterPosture
import Tactic.Squads.Decisions.RetreatVerdict
import Micro.Targeting.BreakTerranWallIn
import Micro.Targeting.EnemyWallInDetected
import Micro.Targeting.ProtossWallIn
import Micro.Targeting.ReaverTargets
import Micro.Targeting.Target
import Micro.Targeting.TargetChaff
import Micro.Targeting.WallBreachTarget
import Information.Battles.ArmyStrength
import Micro.Targeting.WorkerSpread
import Micro.Actions.Combat.Decisioning.Formation.FormationHold
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.Group.GenericFriendlyUnitGroup
import Tactic.Squads.Group.TFriendlyUnitGroup
import Tactic.Squads.Group.UnitGroup
import Utilities.DispatchBand
import Utilities.Time.Forever
import Utilities.?

/**
 * Technique dispatch pipeline (one explicit list, first claim wins):
 * reorder by moving a line, disable by commenting one out. Bands say
 * which arbitration each stage fights in; the sequence below is the
 * contract, pinned by TechniqueDispatchOrderSuite. Every expression
 * below is the old ladder line verbatim - the list changes the shape,
 * never the behavior.
 */
private[Decisioning] object TechniquePipeline {

  /**
   * Unseen killers outrank every formation and duel: a remembered Lurker
   * or cloaked Templar kills the line that rallies or engages through
   * its memory, so the dodge claims before Walk, Aim, retreat and fight.
   * Grounded warriors only: flyers pass over both killers untouched.
   */
  object AvoidStage extends TechniqueBehaviorStage("Avoid", DispatchBand.High) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
      ! unit.flying && unit.unitClass.isWarrior

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      AvoidDarkTemplar.act(unit) || AvoidLurkers.act(unit)
  }

  /**
   * Siege shells outrank everything below: a detected ground unit that
   * does not want the tank battle steps past the shell line before any
   * march, duel or retreat plan runs. Runs before the Fallback potshot
   * on purpose - a sleeping hull claims the potshot without firing and
   * would otherwise pose inside the shells instead of leaving them.
   * The two static keep-offs live one home below
   * (HandleEnemyCombatBuildings), the duel and the retreat below them.
   * Mobile ground hulls: the formation below walks Reavers and
   * detectors through their own disciplines.
   */
  object StaticKeepOffStage extends TechniqueBehaviorStage("StaticKeepOff", DispatchBand.High) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
      ! unit.flying && unit.canMove && ! Protoss.Reaver(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      HandleEnemyCombatBuildings.run(unit, combat, advance)
  }

  /** The technique executes inside the fight its band allows. */
  object WalkStage extends TechniqueBehaviorStage("Walk", DispatchBand.Medium) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = unit.canMove

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      combat.technique == Walk && combat.maneuvers.walk()
  }

  object AimStage extends TechniqueBehaviorStage("Aim", DispatchBand.Medium) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = ! unit.canMove

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      combat.technique == Aim && AimBehavior.run(unit, combat)
  }

  object DodgeStage extends TechniqueBehaviorStage("Dodge", DispatchBand.Medium) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = unit.canMove && ! unit.loaded

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      combat.technique == Dodge && combat.maneuvers.dodge()
  }

  /** Retreat potshot: Fallback shoots while leaving. */
  object PotshotStage extends TechniqueBehaviorStage("Potshot", DispatchBand.Medium) {
    // Any hull the Fallback picks: the pick below owns the gun check,
    // and the stage claims the cadence even with no shot to fire.
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = true

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      combat.technique == Fallback && PotshotBehavior.run(unit, combat)
  }

  /**
   * Always-attack victims outrank retreat: grounded anti-air shoots
   * Scourge, Carriers and the rest on sight - even while retreating.
   * A shot in range never waits for the duel to start or the retreat
   * to finish; the ladder picks, the gun fires, the frame ends here.
   * Grounded warriors: the sky and the helpless never reach for a gun.
   */
  object AlwaysAttackStage extends TechniqueBehaviorStage("AlwaysAttack", DispatchBand.High) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
      ! unit.flying && unit.unitClass.isWarrior

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      AlwaysAttackInRange.run(unit, combat.shouldRetreat, advance)
  }

  /**
   * Main-base Templar hunt: a detected enemy DT inside forty ground
   * tiles of our main must die, even out of contact - the army walks
   * it down instead of scoring shinier fights. Already dueling one
   * in range keeps its duel; Reavers and our own Templars keep
   * their own disciplines.
   */
  object DarkTemplarHuntStage extends TechniqueBehaviorStage("DarkTemplarHunt", DispatchBand.High) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
      ! unit.flying && unit.unitClass.isWarrior && unit.unitClass.attacksGround &&
        ! Protoss.Reaver(unit) && ! Protoss.DarkTemplar(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      DarkTemplarHuntBehavior.run(unit)
  }

  /**
   * A fresh hot Dragoon shoots the Terran in range instead of running
   * from it - but never under siege, the line above already claimed it.
   */
  object DragoonStandStage extends TechniqueBehaviorStage("DragoonStand", DispatchBand.Medium) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = Protoss.Dragoon(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      advance && DragoonStandBehavior.run(unit, combat)
  }

  /**
   * A shielded Dragoon answers a Carrier in range and closes a pathed
   * one inside eleven tiles instead of retreating past the kill.
   */
  object DragoonCarrierStage extends TechniqueBehaviorStage("DragoonCarrier", DispatchBand.Medium) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = Protoss.Dragoon(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      advance && DragoonCarrierBehavior.run(unit, combat)
  }

  /**
   * A Dragoon that spots a detected Spider Mine inside ten tiles
   * shoots it instead of walking into it - but only with no enemy
   * that can answer inside twelve; under guns the duel owns the
   * frame. Advancing only: a retreating hull keeps running.
   */
  object DragoonMineDefuseStage extends TechniqueBehaviorStage("DragoonMineDefuse", DispatchBand.Medium) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = Protoss.Dragoon(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      advance && DragoonMineDefuseBehavior.run(unit)
  }

  /**
   * A covering rearguard shoots victims of opportunity and holds the
   * line while the cluster withdraws instead of turning with it. With
   * nothing to shoot it falls through and retreats with everyone.
   * Any hull the squad covers: the squad owns who holds the line.
   */
  object RearguardStage extends TechniqueBehaviorStage("Rearguard", DispatchBand.Medium) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = true

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      unit.squad.exists(_.coversRetreat(unit.id)) && (AlwaysAttackInRange.run(unit, willRetreat = true, advance = false) || ContainFireBehavior.run(unit))
  }

  /** Retreat owns survival: the walk to safety ends the frame. */
  object RetreatStage extends TechniqueBehaviorStage("Retreat", DispatchBand.High) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = unit.canMove

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      combat.shouldRetreat && Retreat(unit)
  }

  /**
   * Brawl claims only allowed brawls: melee hulls that can move trade
   * blows instead of glitching on re-targets. Ranged hulls never
   * reach for it; the duel below owns their fight.
   */
  object BrawlStage extends TechniqueBehaviorStage("Brawl", DispatchBand.Medium) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
      unit.canMove && (unit.is(Zerg.Zergling) || unit.unitClass.melee)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      combat.shouldEngage && advance && Brawl.allowed(unit) && Brawl(unit)
  }

  /**
   * Abuse owns range: kits that outrange the duel poke instead of
   * trading volleys. Only hulls the doctrine covers - the ladder
   * whitelists abusers, everyone else fights fair.
   */
  object AbuseStage extends TechniqueBehaviorStage("Abuse", DispatchBand.Medium) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = unit.unitClass.abuseAllowed

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      combat.technique == Abuse && advance && AbuseBehavior.run(unit, combat)
  }

  /**
   * Superkite owns the air duel its hulls were built for: Wraiths and
   * Mutalisks dance instead of trading. Nobody else spins up the dance.
   */
  object SuperKiteStage extends TechniqueBehaviorStage("SuperKite", DispatchBand.Medium) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
      unit.isAny(Terran.Wraith, Zerg.Mutalisk)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      combat.technique == SuperKite && advance && SuperkiteBehavior.run(unit, combat)
  }

  val stages: Vector[TechniqueBehaviorStage] = Vector(
    AvoidStage,
    StaticKeepOffStage,
    WalkStage,
    AimStage,
    DodgeStage,
    PotshotStage,
    AlwaysAttackStage,
    DarkTemplarHuntStage,
    DragoonStandStage,
    DragoonCarrierStage,
    DragoonMineDefuseStage,
    RearguardStage,
    RetreatStage,
    BrawlStage,
    AbuseStage,
    SuperKiteStage)
}

final class Combat(unit: FriendlyUnitInfo) extends Action with CombatSituation {

  var restrained            : Boolean = false
  var restrainedFrames      : Double  = 0.0
  var technique             : Technique = Fight
  var framesUntilShot       : Int = Forever()
  var framesToPokeTarget    : Option[Int] = None
  var idealTargetDistance   : Double = _
  var hasSpacetimeToPoke    : Boolean = _
  var breakFormationToAttack: Boolean = _
  var trapped               : Boolean = _

  private[Decisioning] val techniques  = new CombatTechniques(this, unit)
  private[Decisioning] val distances   = new CombatDistances(this, unit)
  private[Decisioning] val maneuvers   = new CombatManeuvers(this, unit)
  private[Decisioning] val engagement  = new CombatEngagement(this, unit)

  private[Decisioning] def group                 : UnitGroup with TFriendlyUnitGroup = unit.squad.orElse(unit.friendlyTeam).getOrElse(new GenericFriendlyUnitGroup(unit))
  private[Decisioning] def target                : Option[UnitInfo]  = unit.agent.toAttack
  private[Decisioning] def squadEngagedUpon      : Boolean = unit.squad.map(_.engagedUpon).getOrElse(unit.matchups.engagedUpon)
  private[Decisioning] def squadEngagingOn       : Boolean = unit.squad.map(_.engagingOn).getOrElse(unit.matchups.engagingOn)
  private[Decisioning] def fightConsensus        : Boolean = unit.squad.map(_.fightConsensus).getOrElse(unit.agent.shouldFight)
  private[Decisioning] def targetFleeing         : Boolean = target.exists(t => t.canMove && Maff.isTowards(unit.pixel, t.pixel, t.angleRadians))
  private[Decisioning] def targetDistanceHere    : Double  = target.map(unit.pixelDistanceEdge).getOrElse(0d)
  private[Decisioning] def targetDistanceThere   : Double  = target.map(unit.pixelDistanceEdgeFrom(_, unit.agent.destinationNext())).getOrElse(0d)
  private[Decisioning] def formationHelpsEngage  : Boolean = targetDistanceThere <= Math.min(targetDistanceHere, target.map(unit.pixelRangeAgainst).getOrElse(0d))
  private[Decisioning] def formationHelpsChase   : Boolean = targetDistanceThere <= targetDistanceHere
  // How much of the engage formation sits in its slots (1.0 = the line is formed).
  // Supplies FormationHold.shouldAdvanceInFormation; empty/missing formation counts as formed so it never blocks.
  private[Decisioning] def formationReadiness    : Double  = unit.formationEngage
    .map(f => FormationHold.fractionInSlot(f.placements.view.map(p => p._1.pixelDistanceCenter(p._2))))
    .getOrElse(1.0)
  private[Decisioning] def idealDistanceForward  : Double  = target.map(unit.pixelDistanceEdge(_) - idealTargetDistance).getOrElse(0d)
  private[Decisioning] def confidentEnoughToChase: Boolean = unit.confidence11 > confidenceChaseThreshold
  private[Decisioning] val confidenceChaseThreshold = 0.25
  private[Decisioning] def shouldChase           : Boolean = (idealDistanceForward > 0 && confidentEnoughToChase && targetFleeing) || (target.exists(_.isAny(Terran.SiegeTankSieged, Protoss.Reaver) && ! With.fingerprints.bio()) && ! unit.flying && ! Protoss.Reaver(unit))
  private[Decisioning] def allThreatsAbusable    : Boolean = unit.matchups.threats.forall(t => canAbuse(t) || t.pixelsToGetInRange(unit) > 24 * 5)
  private[Decisioning] def nudged                : Boolean = unit.agent.receivedPushPriority() > TrafficPriorities.Nudge
  private[Decisioning] def nudgedTowards         : Boolean = nudged && Maff.isTowards(unit.pixel, unit.agent.destinationNext(), unit.agent.receivedPushForce().radians)
  private[Decisioning] def pushThrough           : Boolean = shouldEngage && unit.formationEngage.isDefined && nudged && nudgedTowards && ! unit.flying
  private[Decisioning] def ground10              : Double  = ?(unit.flying, 0, 1)
  private[Decisioning] def shouldRetreat         : Boolean = techniqueIs(Fallback, Flee, Excuse)
  private[Decisioning] def shouldEngage          : Boolean = techniqueIs(Fight, Abuse, Scavenge)
  private[Decisioning] def canAbuse(t: UnitInfo) : Boolean = unit.canAttack(t) && unit.intent.canFight && ( ! t.canAttack(unit) || (unit.topSpeed > t.topSpeed && unit.pixelRangeAgainst(t) > t.pixelRangeAgainst(unit)))

  override def allowed(unit: FriendlyUnitInfo): Boolean = (unit.canMove || unit.canAttack) && unit.intent.canFight
  override def perform(unused: FriendlyUnitInfo = null): Unit = {
    restrained = false
    innerPerform()
    restrainedFrames += ?(restrained, 1.0, -0.5) * With.framesSince(unit.agent.lastFrame)
    restrainedFrames = Math.max(0, restrainedFrames)
  }
  def innerPerform(): Unit = {
    acquireTarget()
    updateVerdicts()
    selectTechnique()
    dispatchByTechnique()
  }

  // A corpse is not a target: the ladder only fills an empty slot, so a
  // stale kill (squad focus included) would pin the unit on thin air
  // forever while real buildings stand next door. Remembered fogged
  // contacts keep alive=true, so only confirmed kills release the slot.
  // Lifted off counts as gone for whoever cannot hit air - a tasked
  // building that takes to the sky is re-picked, not chewed through. A
  // memory is not a contact either: with another hittable target in
  // range the fogged recollection drops and the ladder re-picks, so a
  // zealot posed at a far memory shoots the base a tile away instead.
  private def releaseStaleTarget(): Unit =
    if (unit.agent.toAttack.exists(t =>
      ! t.alive
      || TargetChaff.isChaff(t.unitClass)
      || Target.shouldDropLifted(t.unitClass.isFlyingBuilding, unit.canAttackAir)
      || Target.shouldDropMemory(
        targetVisible = t.visible,
        otherHittableInRange = unit.matchups.targetsInRange.exists(o =>
          o.visible && o.id != t.id && unit.canAttack(o))))) unit.agent.toAttack = None

  /**
   * Worker spread (unit-owned claim): when the ladder picked a worker and
   * more are hittable, deal by attacker id instead of queuing the whole
   * line on the weakest one. Only re-points worker picks - combat targets
   * keep the ladder's word untouched, and a lone worker keeps its duel.
   */
  private def spreadWorkers(): Unit = {
    if ( ! unit.agent.toAttack.exists(_.unitClass.isWorker)) return

    val hittable = unit.matchups.targetsInRange.filter(_.unitClass.isWorker).sortBy(_.id).toVector
    if (hittable.size < 2) return

    val pick = hittable(WorkerSpread.pickSlot(unit.id, hittable.size))
    if ( ! unit.agent.toAttack.contains(pick)) {
      unit.agent.act("SpreadWorkers")
      unit.agent.toAttack = Some(pick)
    }
  }

  private def acquireTarget(): Unit = {
    releaseStaleTarget()
    // Reaver special picks first: vs Zerg the nearest detected Lurker
    // outranks masonry, vs Protoss the nearest Dark Templar does.
    // Loaded and unarmed hulls keep the old targeting untouched.
    if (unit.agent.toAttack.forall(t =>
      ! Zerg.Lurker(t) && ! Protoss.DarkTemplar(t))) ReaverTargets.check(unit)
    if (unit.agent.toAttack.isEmpty) {
      Target.choose(unit)
    }

    WallBreachTarget.run(unit)
    // Detected wall-in fight first: zealots chew the depot nearest the
    // cluster and ranged hulls hold outside it instead of marching in.
    // A Protoss wall gets the same treatment one step down: dragoons
    // chew the building nearest the cluster, wounded hulls hold off.
    // The old breach logic below stays as the fallback for everything
    // neither doctrine claims.
    if ( ! EnemyWallInDetected.check(unit) && ! ProtossWallIn.check(unit)) BreakTerranWallIn.run(unit)
    spreadWorkers()
    Probe.note("Target", unit.id, unit.agent.toAttack.map(_.id.toString).getOrElse("-"))
    unit.agent.choosePerch()
    Commander.defaultEscalation(unit)
  }

  private def updateVerdicts(): Unit = {
    framesUntilShot       = Maff.min((Seq(unit.pixel) ++ unit.agent.perch.pixel).view.flatMap(p =>  unit.matchups.threats.map(_.framesToGetInRange(unit, p)))).getOrElse(Forever())
    framesToPokeTarget    = target.map(unit.framesToLaunchAttack(_) + unit.unitClass.framesToPotshot + With.reaction.agencyMax + With.latency.latencyFrames)
    idealTargetDistance   = distances.getIdealTargetDistance
    hasSpacetimeToPoke    = framesToPokeTarget.exists(_ < framesUntilShot) && unit.agent.perch.pixel.forall(p => ! unit.matchups.threats.exists(t => t.inRangeToAttack(unit, p.project(t.pixel, 16))))
  }

  private def selectTechnique(): Unit = {
    techniques.selectBaseTechnique(safePassage)
    techniques.applyTransitions()
    techniques.syncConsensus()
    Debugging.Probe.note("Tech", unit.id, technique.toString)
  }

  private def safePassage: Boolean =
    unit.matchups.ignorant || unit.topSpeedTransported >= 0.9 * group.meanTopSpeed && unit.matchups.pixelsToThreatRange.forall(_ > 32 * ?(unit.agent.receivedPushPriority() < TrafficPriorities.Bump, 2, 16))

  /**
   * Dispatch order is load-bearing (first claim wins - never reorder
   * without reading every leg below). Bands, top to bottom: HIGH owns
   * survival and unconditional kills (unseen killers, shells, the
   * always-attack ladder, static keep-off); MEDIUM owns the duel
   * (stands, chases, brawls, engages, and the rearguard cover that
   * holds instead of turning); LOW is whatever the squad phase decided
   * before us (march legs, slots, rally). A higher band with a live
   * claim ends the dispatch - lower bands are not even asked, so no
   * threshold inside them can overrule a retreat, a dodge or a snipe.
   */
  private def dispatchByTechnique(): Unit = {

    // The cluster posture bounds the duel claims: Withdraw/Regroup never
    // brawl, abuse, stand or chase - the technique only picks how inside
    // Fight/HoldLine. Squads without a posture keep the full dispatch.
    // The held verdict bounds the duel from below too: posture opens it,
    // the verdict closes it. While held, the advance-gated lines (stands,
    // brawls, abuses, kites, the engage below) do not run and the unit
    // coasts on the march home order, shooting only through the
    // always-attack ladder and the retreat potshot above.
    val posture = unit.squad.flatMap(_.battlePosture)
    val overruled = ClusterPosture.overrulesRetreat(
      posture,
      Information.Battles.ArmyStrength.score(
        With.battles.globalAttack.us.skimStrengthUnits,
        With.battles.globalAttack.enemy.skimStrengthUnits))
    // Committed UMS duelists advance while no retreat is held: the sim
    // forecasts trades, but a losing trade the unit can kite is still a
    // duel, and without advance the technique never runs - the march
    // walks losing duels out of the map instead of kiting them. Same
    // held verdict the march obeys, so both phases agree.
    val umsDuelistAdvances = With.umsMode && unit.agent.commit &&
      ! (RetreatVerdict.held(unit.id, With.frame) && ! overruled)
    val advance = (posture.forall(ClusterPosture.advances) && (! RetreatVerdict.held(unit.id, With.frame) || overruled)) ||
      umsDuelistAdvances

    var i = 0
    var claimed = false
    while (i < TechniquePipeline.stages.length && ! claimed) {
      claimed = TechniquePipeline.stages(i).shouldRun(unit, this, advance)
      i += 1
    }
    // The engage never claims: it runs for side effects when nothing
    // above owned the frame. Probe the gate (change-only): which of the
    // three legs holds the duel out.
    Debugging.Probe.note("Duel", unit.id, s"claimed=$claimed engage=${shouldEngage && advance} ready=${unit.ready} tech=$technique")
    if ( ! claimed && shouldEngage && advance) engagement.engage()
  }

  private[Decisioning] def techniqueIs(candidates: Technique*): Boolean = candidates.contains(technique)
  private[Decisioning] def transition(newTechnique: Technique, predicate: => Boolean): Unit = {
    if (unit.ready && technique.canTransition(newTechnique) && predicate) {
      technique = newTechnique
      // Retreats carry their reason plus both skim shares in parens -
      // a stronger army routing with no enemy in front reads exactly
      // what it voted and at what odds, instead of a bare Retreat.
      unit.agent.act(?(techniqueIs(Fallback, Flee, Excuse),
        Tactic.Squads.Decisions.RetreatVerdict.retreatLabel(
          unit.agent.fightReason,
          With.battles.globalAttack.us.skimStrengthUnits,
          With.battles.globalAttack.enemy.skimStrengthUnits),
        technique.toString))
    }
  }

  def tryDodge(): Boolean = maneuvers.tryDodge()
}
