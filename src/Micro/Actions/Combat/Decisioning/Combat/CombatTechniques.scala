package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Macro.Facts.MacroFacts
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Actions.Combat.Decisioning.Skirmish.NoRetreat
import Tactic.Squads.Decisions.ClusterPosture
import Micro.Actions.Combat.Maneuvering.DOdge
import Micro.Actions.Combat.Spells.Protoss.PsionicStorm
import Micro.Coordination.Pushing.TrafficPriorities
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.UnitFilters.{IsSpeedling, IsSpeedlot}

class CombatTechniques(combat: Combat, unit: FriendlyUnitInfo) {

  /**
   * Roaming Dragoons belong to the march: the roam gate already walks
   * them at the squad goal, and any duel - even a poke - breaks the roam.
   * Suspended here (Fight base and Abuse), so the hull trots through;
   * retreat still works, so pressure still answers. Same facts as the
   * march gate, or the two phases would disagree on what roaming is.
   */
  private def roaming(unit: FriendlyUnitInfo): Boolean =
    Tactic.Squads.Decisions.DragoonRoaming.mayRoam(
      Protoss.Dragoon(unit),
      With.units.enemy.exists(_.unitClass.isProtoss),
      Macro.Facts.MacroFacts.enemyHasShown(Protoss.Dragoon))

  /**
   * Lone hulls march, they do not duel at range: a reinforcing hull
   * with no pack around stops to poke instead of joining the line,
   * and the march/duel flap bleeds the reinforcement dry before it
   * arrives. In contact the duel still owns the frame - a lone hull
   * already trading holds its ground instead of walking through guns.
   */
  private def marchingAlone: Boolean = {
    val nearest = unit.squad.flatMap(squad => Maff.minBy(squad.units.view.filter(u =>
      u != unit && u.alive && u.unitClass.isWarrior))(_.pixelDistanceCenter(unit.pixel))
      .map(a => unit.pixelDistanceCenter(a.pixel) * Maff.inv32))
    nearest.map(t => Tactic.Squads.Decisions.LoneMarch.marchAlone(unit.id, t)).getOrElse(true)
  }

  private[Decisioning] def selectBaseTechnique(safePassage: Boolean): Unit = {
    // Standing-ground lock, computed once per frame: any sacred-ground
    // rule firing (home soil, tanks on the natural, brave Dragoons and
    // Reavers) pins the technique below to neither Fallback nor Flee, so
    // a verdict the never-retreat family just won is never overruled by
    // the technique. The posture floor carries the same vote cluster-wide.
    standGroundNow = NoRetreat.holdsAny(unit)
    // One retreat permission for both phases: the march already obeys
    // RetreatAllowed (strong clusters hold clear fields, dying hulls
    // keep their way out), but the ladder below fell through to a bare
    // Flee with no enemy check at all - marching out with no foe in
    // sight routed every few frames while the march voted to advance.
    // Gating the terminal on the same predicate makes both phases agree
    // by construction. Computed once, shared with the bus update below.
    val unitsScore = Information.Battles.ArmyStrength.score(
      With.battles.globalAttack.us.skimStrengthUnits,
      With.battles.globalAttack.enemy.skimStrengthUnits)
    val nearestArmedTiles = Maff.minBy(With.units.enemy.view.filter(e => e.alive && e.likelyStillThere && e.unitClass.canAttack(unit)))(_.pixelDistanceCenter(unit.pixel))
      .map(e => unit.pixelDistanceCenter(e.pixel) * Maff.inv32).getOrElse(Double.PositiveInfinity)
    val proxyRush = With.enemies.exists(_.isTerran) && With.fingerprints.proxyRax()
    val hardCounterKnown = MacroFacts.enemyHardCounterKnown
    // Single tank verdict (one decider for the guns): the nearest tank
    // of either mode, its reach and dead zone, and the odds on both
    // Single tank verdict (one decider for the guns): the nearest tank
    // of any mode, its reach and dead zone, and the odds evaluated
    // right at the guns - our nearby pack against the tank plus its
    // escorts. Dead-zone hulls shoot for free and cooling hugs walk the
    // tank down; inside reach plus standoff the latched odds decide -
    // commit at two-to-one, leave below three-to-two. Past the band
    // the duel has not started and the verdict abstains, keeping the
    // last call instead of renegotiating the edge. FIGHT falls through
    // to the normal duel below; LEAVE walks out through Fallback.
    // A lonely tank is always walked down to the dead zone.
    val nearestTank = Maff.minBy(unit.matchups.threats.view.filter(t => t.unitClass.isTank))(_.pixelDistanceEdge(unit))
    nearestTank match {
      case None => Tactic.Squads.Decisions.TankVerdict.clear(unit.id)
      case Some(tank) =>
        val reachTiles = tank.pixelRangeAgainst(unit) * Maff.inv32
        val escorts = unit.matchups.threats.view.filter(e =>
          e.id != tank.id &&
          e.alive && e.likelyStillThere &&
          e.unitClass.canAttack(unit) &&
          e.pixelDistanceEdge(tank.pixel) <= Tactic.Squads.Decisions.TankVerdict.escortRadiusTiles * 32.0).toSeq
        val usSkim = With.units.ours.view.filter(o =>
          o.alive && o.unitClass.isWarrior &&
          o.pixelDistanceCenter(unit.pixel) <= Tactic.Squads.Decisions.TankVerdict.packRadiusTiles * 32.0)
          .map(_.subjectiveValue).sum
        val foeSkim = (tank +: escorts).map(_.subjectiveValue).sum
        val fightingTanksNow = Tactic.Squads.Decisions.TankVerdict.held(unit.id).contains(Tactic.Squads.Decisions.TankVerdict.Fight)
        val tankDesire = Tactic.Squads.Decisions.TankVerdict.desire(
          unit.pixelDistanceEdge(tank) * Maff.inv32,
          reachTiles,
          tank.pixelRangeMin * Maff.inv32,
          TankHug.shouldHug(
            if (Terran.SiegeTankSieged(tank)) unit.pixelDistanceEdge(tank) * Maff.inv32 else Double.PositiveInfinity,
            unit.cooldownLeft,
            unit.agent.shouldFight,
            unit.canMove),
          unit.unitClass.isWarrior && unit.canAttack(tank),
          escorts.isEmpty,
          usSkim,
          foeSkim,
          fightingTanksNow)
        tankDesire.foreach(Tactic.Squads.Decisions.TankVerdict.update(unit.id, _))
    }
    val retreatPermitted = Tactic.Squads.Decisions.RetreatAllowed.forUnit(
      unit.totalHealth,
      unitsScore,
      nearestArmedTiles,
      proxyRush,
      hardCounterKnown)
    // Second verdict reader (step 2): the technique ticks the shared bus
    // with frame-fresh inputs and obeys the held verdict. The raw inputs
    // match the old arm one for one; the only behavior change is the hold
    // - a verdict that flickered fight for a frame or two, or a march
    // tick still arriving on the last frame's news, no longer flips the
    // technique out from under the retreat.
    Tactic.Squads.Decisions.RetreatVerdict.update(
      unit.id,
      Tactic.Squads.Decisions.RetreatVerdict.shouldHold(
        unit.unitClass.isWarrior,
        Protoss.Reaver(unit),
        unit.intent.canFlee,
        Protoss.HighTemplar(unit) && PsionicStorm.canCast(unit),
        unit.agent.shouldFight,
        unit.totalHealth,
        unitsScore,
        nearestArmedTiles,
        proxyRush,
        hardCounterKnown,
        Maff.minBy(With.units.enemy.view.filter(e => e.alive && e.unitClass.isWarrior))(_.pixelDistanceCenter(unit.pixel))
          .map(e => unit.pixelDistanceCenter(e.pixel) * Maff.inv32).getOrElse(Double.PositiveInfinity)) &&
        ! Tactic.Squads.Decisions.RetreatVerdict.suppressUmsOpening(With.umsMode, With.frame),
      With.frame)
    // Overwhelming line never routs: when the unit's own squad votes to
    // fight from a clearly superior skim, a latched personal retreat
    // releases immediately instead of waiting for duel confidence that
    // never comes against the few it already outmasses - ten hulls
    // holding off four while the line advances is how grinds stall out.
    // The same release owns the verdict bus below: a stale vote the fog
    // keeps re-arming must not rout a stronger army either. Squads
    // without a posture keep the old latch untouched below.
    val overwhelmingLine = ClusterPosture.overrulesRetreat(
      unit.squad.flatMap(_.battlePosture),
      Information.Battles.ArmyStrength.score(
        With.battles.globalAttack.us.skimStrengthUnits,
        With.battles.globalAttack.enemy.skimStrengthUnits))
    val verdictRetreat = Tactic.Squads.Decisions.RetreatVerdict.heldArmy(With.frame) && ! overwhelmingLine
    combat.technique =
            if ( ! unit.canMove)                                    Fight
      // Latched retreat: once falling back, hold Flee until clear of the
      // deepest threat with margin and the army verdict says fight (or the
      // duel is confident enough to turn inside contact). The latch derives
      // from the verdict, never votes beside it: a local skirmish win must
      // not turn a hull back into a withdrawing army. Units that may
      // not flee keep the old ladder untouched below. Committed hunts
      // never latch: the order already bypassed the vote, so holding
      // the retreat would walk a duel it was sent to fight.
      else  if (unit.intent.canFlee && ! unit.agent.commit && ! standGroundNow && ! overwhelmingLine && ! Tactic.Squads.Decisions.RetreatVerdict.suppressUmsOpening(With.umsMode, With.frame) && ReengageLatch.shouldHoldRetreat(
        combat.techniqueIs(Fallback, Flee, Excuse),
        unit.matchups.pixelsEntangled * Maff.inv32,
        ! Tactic.Squads.Decisions.RetreatVerdict.heldArmy(With.frame),
        unit.confidence11))                                         Flee
      // Held verdict owns the duel above the fresh skirmish vote: the bus
      // carries the last 24 frames both phases already agreed on, so a
      // verdict that just flipped fight under a march still walking home
      // holds Flee instead of charging back through the retreat. Fresh
      // fight with no retreat behind it falls through untouched.
      else  if (verdictRetreat)                                      Flee
      // Single tank verdict (one decider for the guns): LEAVE walks out
      // through Fallback - unconditional inside the footprint unless a
      // dead-zone free kill or a cooling hug says fight, committed past
      // it only with a stronger local and global army. FIGHT falls
      // through to the normal duel below, which closes and duels
      // instead of parking. A transition cannot do this: Fight never
      // transitions into Fallback, so the ladder assigns it directly.
      else  if ( ! standGroundNow &&
        Tactic.Squads.Decisions.TankVerdict.held(unit.id).contains(Tactic.Squads.Decisions.TankVerdict.Leave)) Fallback
      else  if (unit.agent.shouldFight && combat.target.isDefined)   Fight
      else  if (safePassage && combat.target.exists(unit.inRangeToAttack)) Fight
      else  if (safePassage)                                        Walk
      // Templar storm stand: a loaded hull walks into cast range instead
      // of retreating out of it - the cast itself already fired first in
      // the chain, so holding here only keeps it in reach for the next
      // volley. Gated on siege, not race: walking Zerg or unsieged
      // Terran down is allowed danger, walking into siege shells is not
      // (the cast itself still vetoes tank-covered boxes).
      else  if (StormStand.shouldHold(
        Protoss.HighTemplar(unit),
        With.enemies.exists(_.isZerg),
        PsionicStorm.canCast(unit),
        Maff.minBy(unit.matchups.enemies.view.filter(_.alive))(_.pixelDistanceEdge(unit))
          .map(unit.pixelDistanceEdge(_) * Maff.inv32)
          .getOrElse(Double.PositiveInfinity),
        unit.squad.exists(_.fightConsensus),
        With.units.enemy.exists(e =>
          e.alive && e.likelyStillThere && Terran.SiegeTankSieged(e)))) Fight
      else  if ( ! unit.intent.canFlee)                             Walk
      // Standing ground never retreats: the verdict above already
      // voted fight, but with no target picked yet the ladder below
      // would fall through to Flee - hold the line instead and let
      // the engage pick the duel up.
      else  if (standGroundNow)                                       Fight
      // No permission, no retreat: the cluster doctrine above forbids
      // this withdrawal (strong on a clear field, proxy answered not
      // fled), so the frame belongs to the march, not to safety.
      // Dying hulls and known hard counters keep the old Flee.
      else  if (retreatPermitted)                                     Flee
      else                                                            Walk
  }

  /**
   * Dodge discipline: close to our own main a nearly-ready gun holds
   * instead of shuffling - same gate as the execution below, so the
   * label and the move can never disagree.
   */
  private def shouldDodge: Boolean =
    Micro.Actions.Combat.Maneuvering.DOdge.shouldDodge(
      distMainTiles   = Information.Geography.BaseLocations.main.map(base => unit.pixelDistanceCenter(base.heart.center) * Maff.inv32).getOrElse(Double.PositiveInfinity),
      homeRadiusTiles = Information.Geography.Distances.naturalChokeToMainBase(),
      cooldownLeft    = unit.cooldownLeft)

  /**
   * Standing-ground lock (one read per frame, set above): while any
   * never-retreat rule fires the unit holds instead of withdrawing.
   * A `var`, not a `def`: the verdict geography walks the ground path
   * (HomeStand), so every transition below reads the memoized frame
   * value instead of re-walking it.
   */
  private var standGroundNow: Boolean = false

  private[Decisioning] def applyTransitions(): Unit = {
    combat.transition(Aim,       ! unit.canMove)
    combat.transition(Dodge,     unit.agent.receivedPushPriority() >= TrafficPriorities.Dodge && shouldDodge)
    // Abuse never pokes out of a retreat: Fallback already shoots while
    // leaving and the rearguard covers through AlwaysAttack, so the poke
    // dance would only flap against the Flee it just left. Abuse needs
    // something hittable: approaching ghosts with attack-move is the
    // blob charge, not a poke - the duel dresses the line instead.
    combat.transition(Abuse,     unit.unitClass.abuseAllowed && ! roaming(unit) && unit.matchups.targetsInRange.nonEmpty && unit.agent.receivedPushPriority() == TrafficPriorities.Freedom && unit.matchups.targetNearest.exists(combat.canAbuse) && (combat.hasSpacetimeToPoke || combat.allThreatsAbusable) && ! combat.shouldRetreat && combat.framesUntilShot < unit.cooldownMaxAirGround)
    combat.transition(SuperKite, unit.isAny(Terran.Wraith, Zerg.Mutalisk))
    combat.transition(Scavenge,  outmatchedPrey)
    combat.transition(Fallback,  ! standGroundNow && unit.isAny(Terran.SiegeTankUnsieged, Terran.Goliath, Protoss.Reaver) && ! unit.airborne)
    // Unit-owned claims (the predicates live with their units; the order
    // lives here because the technique state machine constrains it).
    combat.transition(Fallback,  ! standGroundNow && Micro.Actions.Protoss.Dragoon.DragoonFallback.facesRush(
      Protoss.Dragoon(unit),
      unit.agent.receivedPushPriority(),
      unit.matchups.targetNearest.exists(unit.inRangeToAttack),
      unit.matchups.threatsInFrames(unit.unitClass.framesToPotshot + 9).forall(_.isAny(Terran.Vulture, IsSpeedlot, Zerg.Zergling)),
      unit.matchups.threatsInFrames(unit.unitClass.framesToPotshot + 9).exists(_.unitClass.ranged)))
    combat.transition(Fallback,  ! standGroundNow && Micro.Actions.Protoss.Zealot.ZealotFallback.outnumberedBySpeedlings(
      unit.isAny(Protoss.Archon, Protoss.Zealot),
      unit.matchups.targetsInRange.exists(IsSpeedling)))
  }

  private def outmatchedPrey: Boolean = (
    combat.target.exists(_.matchups.framesToLive > unit.matchups.framesToLive)
      && unit.matchups.threatDeepest.exists(t => unit.canAttack(t) && unit.pixelRangeAgainst(t) >= t.pixelRangeAgainst(unit))
      && unit.totalHealth * 2.0 <= combat.group.meanAttackerHealth
      && unit.totalHealth * 3.0 <= unit.matchups.threatsInPixels(96).map(_.damageOnNextHitAgainst(unit)).sum
  )

  private[Decisioning] def syncConsensus(): Unit = {
    // The skirmish verdict owns shouldFight: FightOrFlee runs before Fight
    // in Idle, so it already spoke this frame and the technique must not
    // talk over it - that overwrite is exactly the verdict flap the audit
    // measures. A retreat technique still walks the safety destination;
    // it only lost the right to flip the vote.
    if (combat.shouldRetreat) {
      unit.agent.decision.set(unit.agent.safety)
    }
  }
}
