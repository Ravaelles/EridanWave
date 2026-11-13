package Micro.Actions.Protoss.Reaver

/**
 * Sieged-tank doctrine in one kernel (no game access): three distances,
 * three answers. The caller filters sieged tanks; this only measures.
 */
object ReaverTanks {

  /** A sieged tank this close shoots the landed Reaver: evacuate the shot. */
  val aimTiles: Double = 10.0

  /** Inside a sieged tank's reach plus this margin the stand is suicide. */
  val coverMarginTiles: Double = 1.0

  /** A sieged tank this close to the Shuttle counts for the drop veto. */
  val theaterTiles: Double = 20.0

  def aiming(distTiles: Double): Boolean =
    distTiles < aimTiles

  def covering(tankRangeTiles: Double, distTiles: Double): Boolean =
    distTiles <= tankRangeTiles + coverMarginTiles

  def inTheater(distTiles: Double): Boolean =
    distTiles < theaterTiles
}
