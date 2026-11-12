package Micro.Actions.Protoss.DarkTemplar

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
}
