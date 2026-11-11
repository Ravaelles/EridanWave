package Micro.Targeting.FiltersRequired

import Lifecycle.With
import Micro.Targeting.TargetFilter
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

import Tactic.Squads.Group.FriendlyUnitGroup

object TargetFilterFocus extends TargetFilter {

  override def appliesTo(actor: FriendlyUnitInfo): Boolean = actor.targetsAssigned.isDefined

  def legal(actor: FriendlyUnitInfo, target: UnitInfo): Boolean = {
    var output = false
    output ||= actor.agent.commit
    output ||= With.yolo.active
    output ||= ! actor.canMove
    output ||= actor.squad.exists(_.engagedUpon) && target.team.exists(_.engagingOn)
    output ||= actor.targetsAssigned.forall(_.contains(target))
    output ||= actor.squad.exists(canTargetAsRoadblock(_, target))
    // Lone hall cleanup: with no other enemy within 8 tiles the hall is the
    // fight, even out of range and unassigned - walk it down instead of
    // milling past a cleared base.
    output ||= target.unitClass.isTownHall && actor.matchups.enemies.forall(e => e.id == target.id || actor.pixelDistanceEdge(e) > 32 * 8)
    output ||= (actor.inRangeToAttack(target)
      && actor.readyForAttackOrder(target)
      && ( ! target.unitClass.isBuilding || target.unitClass.isStaticDefense))
    output
  }

  def canTargetAsRoadblock(group: FriendlyUnitGroup, target: UnitInfo): Boolean = {
    group.confidence11 < 0.2 && group.roadblocks.contains(target)
  }
}
