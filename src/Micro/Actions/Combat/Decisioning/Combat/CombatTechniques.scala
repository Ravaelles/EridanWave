package Micro.Actions.Combat.Decisioning

import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Coordination.Pushing.TrafficPriorities
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.UnitFilters.{IsSpeedling, IsSpeedlot}

class CombatTechniques(combat: Combat, unit: FriendlyUnitInfo) {

  private[Decisioning] def selectBaseTechnique(safePassage: Boolean): Unit = {
    combat.technique =
            if ( ! unit.canMove)                                    Fight
      else  if (unit.agent.shouldFight && combat.target.isDefined)   Fight
      else  if (safePassage && combat.target.exists(unit.inRangeToAttack)) Fight
      else  if (safePassage)                                        Walk
      else  if ( ! unit.intent.canFlee)                             Walk
      else                                                          Flee
  }

  private[Decisioning] def applyTransitions(): Unit = {
    combat.transition(Aim,       ! unit.canMove)
    combat.transition(Dodge,     unit.agent.receivedPushPriority() >= TrafficPriorities.Dodge)
    combat.transition(Abuse,     unit.unitClass.abuseAllowed && unit.agent.receivedPushPriority() == TrafficPriorities.Freedom && unit.matchups.targetNearest.exists(combat.canAbuse) && (combat.hasSpacetimeToPoke || combat.allThreatsAbusable) && (combat.shouldRetreat || combat.framesUntilShot < unit.cooldownMaxAirGround))
    combat.transition(SuperKite, unit.isAny(Terran.Wraith, Zerg.Mutalisk))
    combat.transition(Scavenge,  outmatchedPrey)
    combat.transition(Fallback,  unit.isAny(Terran.SiegeTankUnsieged, Terran.Goliath, Protoss.Reaver) && ! unit.airborne)
    combat.transition(Fallback,  dragoonFacesRush)
    combat.transition(Fallback,  unit.isAny(Protoss.Archon, Protoss.Zealot) && unit.matchups.targetsInRange.exists(IsSpeedling))
  }

  private def outmatchedPrey: Boolean = (
    combat.target.exists(_.matchups.framesToLive > unit.matchups.framesToLive)
      && unit.matchups.threatDeepest.exists(t => unit.canAttack(t) && unit.pixelRangeAgainst(t) >= t.pixelRangeAgainst(unit))
      && unit.totalHealth * 2.0 <= combat.group.meanAttackerHealth
      && unit.totalHealth * 3.0 <= unit.matchups.threatsInPixels(96).map(_.damageOnNextHitAgainst(unit)).sum
  )

  private def dragoonFacesRush: Boolean = (
    Protoss.Dragoon(unit)
      && unit.agent.receivedPushPriority() < TrafficPriorities.Shove
      && unit.matchups.targetNearest.exists(unit.inRangeToAttack)
      && unit.matchups.threatsInFrames(unit.unitClass.framesToPotshot + 9).forall(_.isAny(Terran.Vulture, IsSpeedlot, Zerg.Zergling))
  )

  private[Decisioning] def syncConsensus(): Unit = {
    if (combat.shouldEngage && ! unit.agent.shouldFight) {
      unit.agent.shouldFight = true
      unit.agent.fightReason = "Technique"
    }
    if (combat.shouldRetreat) {
      unit.agent.decision.set(unit.agent.safety)
      if (unit.agent.shouldFight) {
        unit.agent.shouldFight = false
        unit.agent.fightReason = "Technique"
      }
    }
  }
}
