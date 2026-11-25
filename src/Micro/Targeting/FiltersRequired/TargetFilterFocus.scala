package Micro.Targeting.FiltersRequired

import Lifecycle.With
import Micro.Targeting.TargetOverrideFilter
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

import Tactic.Squads.Group.FriendlyUnitGroup

object TargetFilterFocus extends TargetOverrideFilter {

  override def appliesTo(actor: FriendlyUnitInfo): Boolean = actor.targetsAssigned.isDefined

  /**
   * Building vote of the in-range rule (no game access): a ready shot at a
   * hittable target is always legal - except unassigned non-static
   * buildings, which the squad never asked for. Suicide attackers vote for
   * everything in their base: workers first by ladder order, then combat,
   * then the base itself - standing on a Pylon while workers flee past is
   * the miss this exception fixes, proved in StarEngine before it shipped.
   */
  def allowsBuilding(isBuilding: Boolean, isStaticDefense: Boolean, isSuicideMission: Boolean): Boolean =
    ! isBuilding || isStaticDefense || isSuicideMission

  def legal(actor: FriendlyUnitInfo, target: UnitInfo): Boolean = {
    var output = false
    output ||= actor.agent.commit
    output ||= With.yolo.active
    output ||= ! actor.canMove
    output ||= actor.squad.exists(_.engagedUpon) && target.team.exists(_.engagingOn)
    // Focus dies with its targets: once every assigned target is a confirmed
    // corpse the lock releases and the ladder re-picks (cleared-base
    // buildings included) instead of holding the squad on thin air.
    output ||= actor.targetsAssigned.exists(_.forall(! _.alive))
    output ||= actor.targetsAssigned.forall(_.contains(target))
    output ||= actor.squad.exists(canTargetAsRoadblock(_, target))
    output ||= (actor.inRangeToAttack(target)
      && actor.readyForAttackOrder(target)
      && allowsBuilding(target.unitClass.isBuilding, target.unitClass.isStaticDefense, actor.isSuicideMission))
    output
  }

  def canTargetAsRoadblock(group: FriendlyUnitGroup, target: UnitInfo): Boolean = {
    group.confidence11 < 0.2 && group.roadblocks.contains(target)
  }
}
