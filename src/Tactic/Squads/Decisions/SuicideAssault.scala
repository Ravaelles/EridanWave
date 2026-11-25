package Tactic.Squads.Decisions

/**
 * Never-pose-near-the-enemy-main gate (no game access): a ground warrior
 * this close never retreats, holds formation or dresses the line - it
 * only attacks. Two distance modes in one object: the Zerg charge measures
 * along the ground (Euclidean goes blind where UMS ground does not
 * connect, which is exactly where packs mill around in formation), while
 * the any-race assault fires on straight tile distance for every race.
 * Distances in tiles; ground callers measure along the ground, never as
 * the flies.
 */
object SuicideAssault {

  /** At or below this land distance to the Zerg main the charge is on. */
  val chargeTiles: Double = 30.0

  /** At or below this tile distance to the enemy main the assault is on. */
  val assaultTiles: Double = 15.0

  /** Release past this tile distance: the assault lets go two tiles past
   * the edge instead of flapping on it. */
  val assaultReleaseTiles: Double = 17.0

  /** Release past this land distance: the charge lets go two tiles past
   * the edge instead of flapping on it. */
  val chargeReleaseTiles: Double = 32.0

  def charging(distLandTiles: Double): Boolean =
    distLandTiles <= chargeTiles

  def assaulting(distTiles: Double): Boolean =
    distTiles <= assaultTiles

  /**
   * Fully clear (pure tiles for the suite): past both release bands the
   * mission lets go; inside either it holds. One side out is still in -
   * the other mode may still own the hull.
   */
  def released(assaultTiles: Double, chargeTiles: Double): Boolean =
    assaultTiles > assaultReleaseTiles && chargeTiles > chargeReleaseTiles
}
