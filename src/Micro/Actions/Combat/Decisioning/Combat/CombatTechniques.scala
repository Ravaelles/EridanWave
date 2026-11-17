package Micro.Actions.Combat.Decisioning

import Debugging.Probe
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Techniques._
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

  private[Decisioning] def selectBaseTechnique(safePassage: Boolean): Unit = {
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
        Information.Battles.ArmyStrength.score(
          With.battles.globalAttack.us.skimStrengthUnits,
          With.battles.globalAttack.enemy.skimStrengthUnits),
        Maff.minBy(With.units.enemy.view.filter(e => e.alive && e.likelyStillThere && e.unitClass.attacksGround))(_.pixelDistanceCenter(unit.pixel))
          .map(e => unit.pixelDistanceCenter(e.pixel) * Maff.inv32).getOrElse(Double.PositiveInfinity),
        With.enemies.exists(_.isTerran) && With.fingerprints.proxyRax()),
      With.frame)
    val verdictRetreat = Tactic.Squads.Decisions.RetreatVerdict.held(unit.id, With.frame)
    combat.technique =
            if ( ! unit.canMove)                                    Fight
      // Latched retreat: once falling back, hold Flee until clear of the
      // deepest threat with margin and the verdict says fight (or the
      // duel is confident enough to turn inside contact). Units that may
      // not flee keep the old ladder untouched below.
      else  if (unit.intent.canFlee && ReengageLatch.shouldHoldRetreat(
        combat.techniqueIs(Fallback, Flee, Excuse),
        unit.matchups.pixelsEntangled * Maff.inv32,
        unit.agent.shouldFight,
        unit.confidence11))                                         Flee
      // Held verdict owns the duel above the fresh skirmish vote: the bus
      // carries the last 24 frames both phases already agreed on, so a
      // verdict that just flipped fight under a march still walking home
      // holds Flee instead of charging back through the retreat. Fresh
      // fight with no retreat behind it falls through untouched.
      else  if (verdictRetreat)                                      Flee
      else  if (unit.agent.shouldFight && combat.target.isDefined)   Fight
      else  if (safePassage && combat.target.exists(unit.inRangeToAttack)) Fight
      else  if (safePassage)                                        Walk
      // Templar storm stand: a loaded hull walks into cast range instead
      // of retreating out of it - the cast itself already fired first in
      // the chain, so holding here only keeps it in reach for the next
      // volley. Gated on Zerg: walking hydras down is the doctrine's
      // home ground, other matchups keep the old retreat.
      else  if (StormStand.shouldHold(
        Protoss.HighTemplar(unit),
        With.enemies.exists(_.isZerg),
        PsionicStorm.canCast(unit),
        Maff.minBy(unit.matchups.enemies.view.filter(_.alive))(_.pixelDistanceEdge(unit))
          .map(unit.pixelDistanceEdge(_) * Maff.inv32)
          .getOrElse(Double.PositiveInfinity),
        unit.squad.exists(_.fightConsensus)))                       Fight
      else  if ( ! unit.intent.canFlee)                             Walk
      else                                                          Flee
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

  private[Decisioning] def applyTransitions(): Unit = {
    combat.transition(Aim,       ! unit.canMove)
    combat.transition(Dodge,     unit.agent.receivedPushPriority() >= TrafficPriorities.Dodge && shouldDodge)
    // Abuse never pokes out of a retreat: Fallback already shoots while
    // leaving and the rearguard covers through AlwaysAttack, so the poke
    // dance would only flap against the Flee it just left.
    combat.transition(Abuse,     unit.unitClass.abuseAllowed && ! roaming(unit) && unit.agent.receivedPushPriority() == TrafficPriorities.Freedom && unit.matchups.targetNearest.exists(combat.canAbuse) && (combat.hasSpacetimeToPoke || combat.allThreatsAbusable) && ! combat.shouldRetreat && combat.framesUntilShot < unit.cooldownMaxAirGround)
    combat.transition(SuperKite, unit.isAny(Terran.Wraith, Zerg.Mutalisk))
    combat.transition(Scavenge,  outmatchedPrey)
    combat.transition(Fallback,  unit.matchups.inRangeOfTank && TankHug.shouldWithdraw(
      With.framesSince(unit.lastFrameTakingDamage) < TankHug.shellShockFrames,
      Maff.minBy(unit.matchups.threats.view.filter(Terran.SiegeTankSieged))(_.pixelDistanceEdge(unit))
        .map(unit.pixelDistanceEdge(_) * Maff.inv32)
        .getOrElse(Double.PositiveInfinity)))
    combat.transition(Fallback,  unit.isAny(Terran.SiegeTankUnsieged, Terran.Goliath, Protoss.Reaver) && ! unit.airborne)
    // Unit-owned claims (the predicates live with their units; the order
    // lives here because the technique state machine constrains it).
    combat.transition(Fallback,  Micro.Actions.Protoss.Dragoon.DragoonFallback.facesRush(
      Protoss.Dragoon(unit),
      unit.agent.receivedPushPriority(),
      unit.matchups.targetNearest.exists(unit.inRangeToAttack),
      unit.matchups.threatsInFrames(unit.unitClass.framesToPotshot + 9).forall(_.isAny(Terran.Vulture, IsSpeedlot, Zerg.Zergling))))
    combat.transition(Fallback,  Micro.Actions.Protoss.Zealot.ZealotFallback.outnumberedBySpeedlings(
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
    Debugging.Probe.note("Technique", unit.id, combat.technique.toString)
    shadowVerdict()
  }

  // Unified-verdict shadow (step 1, no behavior change): the union of the
  // march retreat arm and the technique march arm, computed beside them
  // from the same inputs. Nothing reads the result; disagreements against
  // today's two readers probe out for the next validation game. The march
  // arm ignores canFlee and the storm hold, so committed hulls and casting
  // Templars are the expected shadow-vs-march shape; the technique arms
  // add the latch, so shadow-vs-technique fires wherever the latch holds
  // a retreat the raw inputs released (or the reverse).
  private def shadowVerdict(): Unit = {
    val shadow = Tactic.Squads.Decisions.RetreatVerdict.shouldHold(
      unit.unitClass.isWarrior,
      Protoss.Reaver(unit),
      unit.intent.canFlee,
      Protoss.HighTemplar(unit) && PsionicStorm.canCast(unit),
      unit.agent.shouldFight,
      unit.totalHealth,
      Information.Battles.ArmyStrength.score(
        With.battles.globalAttack.us.skimStrengthUnits,
        With.battles.globalAttack.enemy.skimStrengthUnits),
      Maff.minBy(With.units.enemy.view.filter(e => e.alive && e.likelyStillThere && e.unitClass.attacksGround))(_.pixelDistanceCenter(unit.pixel))
        .map(e => unit.pixelDistanceCenter(e.pixel) * Maff.inv32).getOrElse(Double.PositiveInfinity),
      With.enemies.exists(_.isTerran) && With.fingerprints.proxyRax())
    val marchHome   = unit.intent.marchHome
    val techRetreat = combat.shouldRetreat
    if (shadow != marchHome || shadow != techRetreat) {
      Debugging.Probe.note(
        "RetreatShadow",
        unit.id,
        f"shadow=$shadow marchHome=$marchHome techRetreat=$techRetreat tech=${combat.technique}")
    }
  }
}
