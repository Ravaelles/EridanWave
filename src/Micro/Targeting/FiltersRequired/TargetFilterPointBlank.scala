package Micro.Targeting.FiltersRequired

import Micro.Targeting.TargetOverrideFilter
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Point-blank override: a building we can already hit is always a legal
 * target, assigned or not - holding fire on a Hatchery a tile away
 * while chasing a Drone across the base is how armies watch instead
 * of razing. Statics that outrange us stay exempt: walking onto a
 * Sunken is still suicide, the Reaver or the squad assignment
 * handles those.
 */
object TargetFilterPointBlank extends TargetOverrideFilter {

  override def appliesTo(actor: FriendlyUnitInfo): Boolean = actor.targetsAssigned.isDefined

  /** Reach inside which a building is point-blank, in tiles past our own. */
  val pointBlankTiles: Double = 1.0

  /**
   * Point-blank release (pure numbers for the suite): a hittable building
   * beside us outranks any assignment lock. The caller feeds live range;
   * this only combines it.
   */
  def pointBlankLegal(targetIsBuilding: Boolean, targetOutrangesUs: Boolean, distPixels: Double, ownRangePixels: Double): Boolean =
    targetIsBuilding && ! targetOutrangesUs && distPixels <= ownRangePixels + pointBlankTiles * 32.0

  def legal(actor: FriendlyUnitInfo, target: UnitInfo): Boolean =
    pointBlankLegal(
      targetIsBuilding  = target.unitClass.isBuilding,
      targetOutrangesUs = target.pixelRangeAgainst(actor) > actor.pixelRangeAgainst(target),
      distPixels        = actor.pixelDistanceEdge(target),
      ownRangePixels    = actor.pixelRangeAgainst(target))
}
