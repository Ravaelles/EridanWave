package Micro.Targeting.FiltersRequired

import Micro.Targeting.TargetOverrideFilter
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Lone-hall override: with no other reachable enemy within eight tiles
 * the hall is the fight, even out of range and unassigned - walk it down
 * instead of milling past a cleared base. Stale contacts and fliers
 * don't count: an Overlord parked overhead must never veto the hall.
 */
object TargetFilterHallCleanup extends TargetOverrideFilter {

  override def appliesTo(actor: FriendlyUnitInfo): Boolean = actor.targetsAssigned.isDefined

  /** Reach inside which another enemy vetoes the lone-hall cleanup, in tiles. */
  val hallThreatTiles: Double = 8.0

  def loneHallCleanup(targetIsTownHall: Boolean, otherThreatNear: Boolean): Boolean =
    targetIsTownHall && ! otherThreatNear

  def legal(actor: FriendlyUnitInfo, target: UnitInfo): Boolean =
    loneHallCleanup(
      target.unitClass.isTownHall,
      actor.matchups.enemies.exists(e =>
        e.id != target.id && e.visible && ! e.flying && actor.pixelDistanceEdge(e) <= hallThreatTiles * 32.0))
}
