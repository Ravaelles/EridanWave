package Mathematics

import Mathematics.Points.Pixel

/**
 * Same-type separation (no game access): stacked hulls of one class eat the
 * same shot together, so the newer one slides out along the ray while the
 * older holds its ground. One rule for eyes, wings and arbiters - only the
 * radius differs, and the caller owns it.
 */
object Separation {

  /** Another hull of our own type, already measured. */
  final case class Neighbor(id: Int, pixel: Pixel, distTiles: Double)

  def shouldSpread(usId: Int, olderId: Int, distTiles: Double, minTiles: Double): Boolean =
    olderId < usId && distTiles < minTiles

  /** The nearest older hull, if any: only older neighbors count, the newer moves. */
  def partner(usId: Int, others: Seq[Neighbor]): Option[Neighbor] =
    Maff.minBy(others.filter(_.id < usId))(_.distTiles)

  /** Full separation along the ray from the older hull through us. */
  def awayFrom(us: Pixel, them: Pixel, minTiles: Double): Pixel =
    them.project(us, minTiles * 32.0)

  /**
   * Attack hold (pure flags for the suite): a hull that is shooting or
   * about to keeps its firing line instead of sliding off the ray - the
   * spread exists to dodge shared shots while repositioning, not to walk
   * a dueling wing out of its own kill.
   */
  def holdForAttack(hasTarget: Boolean, coolingDown: Boolean): Boolean =
    hasTarget || coolingDown
}
