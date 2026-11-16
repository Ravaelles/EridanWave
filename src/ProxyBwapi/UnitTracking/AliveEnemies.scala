package ProxyBwapi.UnitTracking

import Lifecycle.With
import Micro.Targeting.TargetChaff
import ProxyBwapi.UnitInfo.UnitInfo

/**
 * One cached registry of alive enemies, recomputed at most once per frame.
 * Targeting scans base on this instead of re-filtering the tracker, so a
 * corpse lingering a frame in the buffers never leaks into any calculation.
 * Hatch chaff (Larvae, Eggs) never enters: it cannot fight, so no duel,
 * count or scan should ever see it as an enemy.
 */
object AliveEnemies {

  private var frame: Int = -1
  private var cache: Vector[UnitInfo] = Vector.empty

  /** A live enemy worth knowing about: alive and never hatch chaff. */
  def isValidTarget(unit: UnitInfo): Boolean =
    unit.alive && ! TargetChaff.isChaff(unit.unitClass)

  def get(): Vector[UnitInfo] = {
    if (frame != With.frame) {
      frame = With.frame
      cache = With.units.enemy.filter(isValidTarget).toVector
    }
    cache
  }
}
