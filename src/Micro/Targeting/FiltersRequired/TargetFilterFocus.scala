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
    // Focus dies with its targets: once every assigned target is a confirmed
    // corpse the lock releases and the ladder re-picks (cleared-base
    // buildings included) instead of holding the squad on thin air.
    output ||= actor.targetsAssigned.exists(_.forall(! _.alive))
    output ||= actor.targetsAssigned.forall(_.contains(target))
    output ||= actor.squad.exists(canTargetAsRoadblock(_, target))
    // Lone hall cleanup: with no other reachable enemy within 8 tiles the
    // hall is the fight, even out of range and unassigned - walk it down
    // instead of milling past a cleared base. Stale contacts and fliers
    // don't count: an Overlord parked overhead must never veto the hall.
    output ||= target.unitClass.isTownHall && actor.matchups.enemies.forall(e =>
      e.id == target.id || ! e.visible || e.flying || actor.pixelDistanceEdge(e) > 32 * 8)
    output ||= (actor.inRangeToAttack(target)
      && actor.readyForAttackOrder(target)
      && ( ! target.unitClass.isBuilding || target.unitClass.isStaticDefense))
    // Point-blank bust: a building we can already hit is always a legal
    // target, assigned or not - holding fire on a Hatchery a tile away
    // while chasing a Drone across the base is how armies watch instead
    // of razing. Statics that outrange us stay exempt: walking onto a
    // Sunken is still suicide, the Reaver or the squad assignment
    // handles those.
    output ||= pointBlankLegal(
      targetIsBuilding  = target.unitClass.isBuilding,
      targetOutrangesUs = target.pixelRangeAgainst(actor) > actor.pixelRangeAgainst(target),
      distPixels        = actor.pixelDistanceEdge(target),
      ownRangePixels    = actor.pixelRangeAgainst(target))
    // Wall-bust: with no enemy inside our shot plus a 1.5-tile margin and
    // no cooldown running, any building is a legal target - a Terran
    // wall-in (or any blocking base) must eat shots instead of holding
    // the army idle while it waits for a fight that never comes. Never
    // into outranging statics: a Dragoon walking on a Sunken Colony is
    // not breaching, it is donating - whoever outranges it handles that.
    output ||= wallBustLegal(
      coolingDown         = actor.cooldownLeft > 0,
      noEnemyInsideMargin = actor.matchups.enemies.forall(e =>
        e.id == target.id
        || actor.pixelDistanceEdge(e) >= actor.pixelRangeAgainst(e) + wallBustMarginTiles * 32.0),
      targetIsBuilding    = target.unitClass.isBuilding,
      targetOutrangesUs   = target.pixelRangeAgainst(actor) > actor.pixelRangeAgainst(target))
    output
  }

  /** Shot margin inside which any enemy vetoes the wall-bust, in tiles. */
  val wallBustMarginTiles: Double = 1.5

  /**
   * Wall-bust release (pure numbers for the suite): idle guns with a clear
   * foreground break blockers instead of posing. The caller feeds live
   * range checks; this only combines them.
   */
  def wallBustLegal(coolingDown: Boolean, noEnemyInsideMargin: Boolean, targetIsBuilding: Boolean, targetOutrangesUs: Boolean): Boolean =
    ! coolingDown && noEnemyInsideMargin && targetIsBuilding && ! targetOutrangesUs

  /** Reach inside which a building is point-blank, in tiles past our own. */
  val pointBlankTiles: Double = 1.0

  /**
   * Point-blank release (pure numbers for the suite): a hittable building
   * beside us outranks any assignment lock. The caller feeds live range;
   * this only combines it.
   */
  def pointBlankLegal(targetIsBuilding: Boolean, targetOutrangesUs: Boolean, distPixels: Double, ownRangePixels: Double): Boolean =
    targetIsBuilding && ! targetOutrangesUs && distPixels <= ownRangePixels + pointBlankTiles * 32.0

  def canTargetAsRoadblock(group: FriendlyUnitGroup, target: UnitInfo): Boolean = {
    group.confidence11 < 0.2 && group.roadblocks.contains(target)
  }
}
