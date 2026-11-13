package Micro.Targeting.FiltersRequired

import Micro.Targeting.TargetOverrideFilter
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Wall-bust override: with no enemy inside our shot plus a 1.5-tile margin
 * and no cooldown running, any building is a legal target - a Terran
 * wall-in (or any blocking base) must eat shots instead of holding
 * the army idle while it waits for a fight that never comes. Never
 * into outranging statics: a Dragoon walking on a Sunken Colony is
 * not breaching, it is donating - whoever outranges it handles that.
 */
object TargetFilterWallBust extends TargetOverrideFilter {

  override def appliesTo(actor: FriendlyUnitInfo): Boolean = actor.targetsAssigned.isDefined

  /** Shot margin inside which any enemy vetoes the wall-bust, in tiles. */
  val wallBustMarginTiles: Double = 1.5

  /**
   * Wall-bust release (pure numbers for the suite): idle guns with a clear
   * foreground break blockers instead of posing. The caller feeds live
   * range checks; this only combines them.
   */
  def wallBustLegal(coolingDown: Boolean, noEnemyInsideMargin: Boolean, targetIsBuilding: Boolean, targetOutrangesUs: Boolean): Boolean =
    ! coolingDown && noEnemyInsideMargin && targetIsBuilding && ! targetOutrangesUs

  def legal(actor: FriendlyUnitInfo, target: UnitInfo): Boolean =
    wallBustLegal(
      coolingDown         = actor.cooldownLeft > 0,
      noEnemyInsideMargin = actor.matchups.enemies.forall(e =>
        e.id == target.id
        || actor.pixelDistanceEdge(e) >= actor.pixelRangeAgainst(e) + wallBustMarginTiles * 32.0),
      targetIsBuilding    = target.unitClass.isBuilding,
      targetOutrangesUs   = target.pixelRangeAgainst(actor) > actor.pixelRangeAgainst(target))
}
