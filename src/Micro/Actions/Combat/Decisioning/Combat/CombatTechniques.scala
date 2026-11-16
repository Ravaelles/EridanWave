package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Actions.Combat.Spells.Protoss.PsionicStorm
import Micro.Coordination.Pushing.TrafficPriorities
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.UnitFilters.{IsSpeedling, IsSpeedlot}

class CombatTechniques(combat: Combat, unit: FriendlyUnitInfo) {

  private[Decisioning] def selectBaseTechnique(safePassage: Boolean): Unit = {
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

  private[Decisioning] def applyTransitions(): Unit = {
    combat.transition(Aim,       ! unit.canMove)
    combat.transition(Dodge,     unit.agent.receivedPushPriority() >= TrafficPriorities.Dodge)
    combat.transition(Abuse,     unit.unitClass.abuseAllowed && unit.agent.receivedPushPriority() == TrafficPriorities.Freedom && unit.matchups.targetNearest.exists(combat.canAbuse) && (combat.hasSpacetimeToPoke || combat.allThreatsAbusable) && (combat.shouldRetreat || combat.framesUntilShot < unit.cooldownMaxAirGround))
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
    if (combat.shouldEngage && ! unit.agent.shouldFight) {
      unit.agent.decideFight(true, "Technique")
    }
    if (combat.shouldRetreat) {
      unit.agent.decision.set(unit.agent.safety)
      if (unit.agent.shouldFight) {
        unit.agent.decideFight(false, "Technique")
      }
    }
  }
}
