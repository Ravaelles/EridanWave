package Micro.Actions.Protoss.ReaverShuttle

/**
 * Pair glue (no game access): an unloaded Reaver and its Shuttle never
 * separate. Either they operate together inside board reach, or they
 * retreat together - the ride never flies away while its hull dies.
 *
 * Two numbers:
 * - glueBandTiles: past this the bond is broken (different bases,
 *   stale reservation) and the old solo rules apply;
 * - hoverCapTiles: the farthest the ride ever hovers from an unloaded
 *   mate - board reach (64 px), the distance the Reaver brain assumes
 *   for rescue and reboard. Under fire the ride still stands off, but
 *   never past the cap; with no threats the hover is the mate itself.
 */
object PairGlue {

  /** Past this the mate is not ours to glue (broken bond, far base). */
  val glueBandTiles: Double = 15.0

  /** Farthest the ride ever hovers from its unloaded mate, in tiles. */
  val hoverCapTiles: Double = 2.0

  def inGlueBand(mateOnFoot: Boolean, mateDistTiles: Double): Boolean =
    mateOnFoot && mateDistTiles <= glueBandTiles

  /**
   * Retreat destination, pure geometry: a pressured ride with its mate on
   * foot inside the band retreats TO the mate (pickup on arrival, then
   * both fly), never away from it. Past the band the bond is broken and
   * the caller keeps its own safety.
   */
  def fleeToMate(mateOnFoot: Boolean, mateDistTiles: Double): Boolean =
    inGlueBand(mateOnFoot, mateDistTiles)

  /**
   * Hover cap, pure geometry: pull a hover point back inside the cap
   * around the mate. Points already inside pass through untouched, so a
   * safe hover on the hull itself never moves.
   */
  def capHover(
    mateX: Double, mateY: Double,
    hoverX: Double, hoverY: Double,
    capTiles: Double = hoverCapTiles): (Double, Double) = {
    val dx = hoverX - mateX
    val dy = hoverY - mateY
    val dist = Math.sqrt(dx * dx + dy * dy)
    if (dist <= capTiles || dist <= 0.0) return (hoverX, hoverY)

    val scale = capTiles / dist
    (mateX + dx * scale, mateY + dy * scale)
  }
}
