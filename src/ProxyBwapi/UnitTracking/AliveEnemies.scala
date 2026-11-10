package ProxyBwapi.UnitTracking

import Lifecycle.With
import ProxyBwapi.UnitInfo.UnitInfo

/**
 * One cached registry of alive enemies, recomputed at most once per frame.
 * Targeting scans base on this instead of re-filtering the tracker, so a
 * corpse lingering a frame in the buffers never leaks into any calculation.
 */
object AliveEnemies {

  private var frame: Int = -1
  private var cache: Vector[UnitInfo] = Vector.empty

  def get(): Vector[UnitInfo] = {
    if (frame != With.frame) {
      frame = With.frame
      cache = With.units.enemy.filter(_.alive).toVector
    }
    cache
  }
}
