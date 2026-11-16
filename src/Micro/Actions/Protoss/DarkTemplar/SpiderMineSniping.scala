package Micro.Actions.Protoss.DarkTemplar

import Mathematics.Points.Pixel

/**
 * Spider Mine sniping: when a lone mine is worth answering. Pure decision on
 * a precomputed sighting, so the snipe rule can be argued with in a test
 * instead of in a game; the game reads stay in BeDarkTemplar.
 */
object SpiderMineSniping {

  /** Only a mine this close, in pixels, is worth answering. */
  val radiusPixels: Int = 96

  final case class MineSighting(
    targetedUs        : Boolean,
    distPixels        : Double,
    neighborsInRadius : Int,
    visible           : Boolean,
    cloaked           : Boolean)

  def shouldSnipe(m: MineSighting, radius: Int = radiusPixels): Boolean =
    (m.targetedUs || (m.distPixels < radius && m.neighborsInRadius == 1)) && m.visible && ! m.cloaked

  /**
   * Sidestep radius, in tiles: known mines this close bend the path.
   * Mines are sniped or stepped around, never marched through - even a
   * seen minefield costs hulls when the line walks it straight.
   */
  val sidestepTiles: Double = 6.0

  /** Sidestep stride, in tiles: full-speed lateral slide, like the flee legs. */
  val sidestepStepTiles: Double = 4.0

  /**
   * Lateral step around the mine centroid (pure pixels): perpendicular
   * to the centroid-to-hull ray, never radial - backing straight out
   * of a ring walks into the mine behind. The caller picks the side
   * once per hull (unit id parity keeps it flap-free); the step lands
   * on the caller to clamp walkable.
   */
  def sidestepPixel(dtPixel: Pixel, centroidPixel: Pixel, stepTiles: Double, goLeft: Boolean): Pixel = {
    val dx = dtPixel.x - centroidPixel.x
    val dy = dtPixel.y - centroidPixel.y
    val dist = Math.sqrt(dx * dx + dy * dy)
    if (dist <= 0) return dtPixel

    val step = stepTiles * 32.0
    // Screen pixels grow downward, so the left of the away-ray is
    // (dy, -dx): facing north away from the centroid below, left is west.
    val (px, py) =
      if (goLeft) (dy / dist * step, -dx / dist * step)
      else (-dy / dist * step, dx / dist * step)

    new Pixel((dtPixel.x + px).toInt, (dtPixel.y + py).toInt)
  }
}
