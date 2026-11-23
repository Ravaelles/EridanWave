package Micro.Actions.Protoss.DarkTemplar

import Mathematics.Points.Pixel
import Utilities.Latch

/**
 * Static-gun standoff for a hunting Dark Templar (no game access): a
 * detector gun outranges the Templar's eyes, so an unseen Cannon owns
 * the ground before the detector arm can fire. Inside the gun's eyes
 * plus a critical margin a closing hull stops instead of walking in;
 * inside the wider close band - stopped or merely close - it backs off
 * in 0.2-tile strides straight off the gun. The band opens past the
 * gun's own sight on purpose: the tile burns at sight, so a reach-sized
 * band would sit entirely inside the burn and never fire first - the
 * standoff owns the approach on memory alone, before any eye is seen
 * and before the tile can burn. The stride never donates a
 * volley the hunt could keep: a hittable prey in reach still dies, the
 * hold only owns empty ground. Latched LoneMarch shape: enter sharp,
 * release past the margin, so the hunt cannot re-trigger on the edge.
 * Distances in tiles, pixels only at the stride seam.
 */
object DarkTemplarStaticStandoff {

  /** Inside gun eyes plus this margin a closing hull must not advance, in tiles. */
  val criticalMarginTiles: Double = 1.0

  /** Inside gun eyes plus this margin the hull backs off instead, in tiles. */
  val closeMarginTiles: Double = 2.5

  /**
   * Hard floor past any gun's reach, in tiles: the ladder bill is two
   * Templars eating a guard volley the frame they entered detection, so
   * no band above may ever admit closer than this to a Cannon. Both
   * bands clear it by construction; the suites pin it, not the comments.
   */
  val neverCloserTiles: Double = 7.4

  /** Exit margin past the close band that releases the standoff, in tiles. */
  val exitMarginTiles: Double = 1.0

  /** One back-off stride directly away from the gun, in tiles. */
  val stepTiles: Double = 0.2

  def dangerTiles(rangeTiles: Double, sightTiles: Double): Double =
    Math.max(rangeTiles, sightTiles)

  /**
   * Stop (pure flags for the suite): critically close and still closing
   * with the back turned to the hunt, not to the gun. A hull already
   * showing its back is leaving - stopping it would re-face the guns,
   * so the stride below owns that frame instead.
   */
  def shouldStop(distTiles: Double, dangerTiles: Double, approaching: Boolean, showingBack: Boolean): Boolean =
    approaching && ! showingBack && distTiles <= dangerTiles + criticalMarginTiles

  /**
   * Stride (pure flags for the suite): the stop above owns its frames,
   * everything else inside the close band backs off - stopped hulls,
   * leaving hulls, and close-but-not-critical approaches alike.
   */
  def shouldStep(distTiles: Double, dangerTiles: Double, approaching: Boolean, showingBack: Boolean): Boolean =
    ! shouldStop(distTiles, dangerTiles, approaching, showingBack) && distTiles <= dangerTiles + closeMarginTiles

  def stepAway(from: Pixel, gun: Pixel): Pixel =
    from.project(gun, -stepTiles * 32.0)

  private val standing = new Latch

  def reset(): Unit = { standing.reset() }

  def standoffLatched(unitId: Int, distTiles: Double, dangerTiles: Double, approaching: Boolean, showingBack: Boolean): Boolean =
    standing(
      unitId,
      fires    = shouldStop(distTiles, dangerTiles, approaching, showingBack) || shouldStep(distTiles, dangerTiles, approaching, showingBack),
      releases = distTiles > dangerTiles + closeMarginTiles + exitMarginTiles,
      initial  = shouldStop(distTiles, dangerTiles, approaching, showingBack) || shouldStep(distTiles, dangerTiles, approaching, showingBack))
}
