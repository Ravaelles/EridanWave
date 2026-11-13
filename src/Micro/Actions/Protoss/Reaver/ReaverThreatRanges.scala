package Micro.Actions.Protoss.Reaver

/**
 * Every "an enemy is this close to the Reaver body" radius in one table.
 * Sieged-tank distances live in ReaverTanks; Shuttle rings answer for the
 * drop, not the body, and stay in ReaverDropBravery; contain radii are
 * tactic state and stay out entirely.
 */
object ReaverThreatRanges {

  /** Ground hittables this close press the Reaver instead of kiting it. */
  val pressuredCloseTiles: Double = 4.0

  /** Hittables this close hold the shot instead of chasing it. */
  val holdTargetTiles: Double = 7.25

  /** Enemies this close, in tiles, are engaged instead of fled. */
  val engageTiles: Double = 8.5

  /** Enemies this close, in tiles, decide worker-only standing. */
  val workerStandTiles: Double = 8.0

  def inReach(distTiles: Double): Boolean =
    distTiles <= engageTiles
}
