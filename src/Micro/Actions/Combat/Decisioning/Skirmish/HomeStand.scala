package Micro.Actions.Combat.Decisioning.Skirmish

import Mathematics.Maff
import Mathematics.Points.Pixel

/**
 * Protoss home stand (thin memo over pure numbers): between our main and
 * the natural choke minus a margin a Protoss unit never retreats - it is
 * already home, so falling back only donates ground. The choke distance
 * is memoized on its endpoints (stable within a game, recomputed across
 * games) instead of re-walked every frame.
 */
object HomeStand {

  /** Hold home this far inside the natural choke, in tiles. */
  val chokeMarginTiles: Double = 4.0

  def holdsGround(unitDistTiles: Double, chokeDistTiles: Double): Boolean =
    unitDistTiles <= chokeDistTiles - chokeMarginTiles

  private var key: Option[(Pixel, Pixel)] = None
  private var distTiles: Double = 0.0

  /** Ground tiles from the main heart to the choke center, memoized. */
  def chokeDistToMain(mainHeart: Pixel, chokeCenter: Pixel): Double = {
    if (key != Some((mainHeart, chokeCenter))) {
      key = Some((mainHeart, chokeCenter))
      distTiles = mainHeart.groundPixels(chokeCenter) * Maff.inv32
    }

    distTiles
  }
}
