package Micro.Actions.Combat.Decisioning.Kiting

import Utilities.?

/**
 * Cluster standoff vs defensive buildings (no game access).
 *
 * When the cluster takes fire, idle units yield an extra tile from
 * defensive buildings instead of standing in range absorbing hits.
 * Pure numbers so the window and the geometry stay unit-testable;
 * Combat supplies the game-aware seam (who was hit, who threatens).
 */
object ClusterSafety {

  /** A cluster mate hit within this window means we are under fire. */
  var attackedWindowFrames: Int = 3 * 24

  /** Extra standoff tiles vs defensive buildings while under fire. */
  var extraStandoffTiles: Double = 1.0

  def recentlyAttacked(framesSinceHit: Int): Boolean =
    framesSinceHit < attackedWindowFrames

  /**
   * @param distPixels current distance to the threat
   * @param threatRangePixels the threat's range against us
   * @param marginTiles base standoff margin
   * @param clusterAttacked whether the cluster recently took fire
   * @return pixels to yield to stand clear, 0 when already clear
   */
  def standoffGapPixels(
    distPixels        : Double,
    threatRangePixels : Double,
    marginTiles       : Double,
    clusterAttacked   : Boolean): Double = {
    val margin = marginTiles + ?(clusterAttacked, extraStandoffTiles, 0.0)

    Math.max(0.0, threatRangePixels + margin * 32 - distPixels)
  }
}
