package Tactic.Squads.Decisions

import Utilities.Latch

/**
 * Catch-up march (pure numbers, no game access): a unit trailing the
 * alpha centroid by more than five tiles to the enemy anchor is joining,
 * not leading - it marches straight at the squad goal instead of
 * rallying, slotting or dressing. The anchor is the nearest ground enemy
 * to the alpha, measured once per march; the unit reads its own distance
 * to the same pixel, so both sides compare against one point. Latched
 * LoneMarch shape: fires six tiles back, releases inside four, holds its
 * side in between - a breathing anchor must not flap the destination.
 * Clear-of-guns stays with ArmedCordon (visible guns only, its own 21/19
 * band): fogged memories never pin a reinforcement, and the composed
 * release inherits the cordon's band.
 */
object CatchUpMarch {

  /** Trailing the alpha by this much (tiles), the unit is joining, not leading. */
  val behindMarginTiles: Double = 5.0

  /** Fires this far back, in tiles. */
  val behindEnterTiles: Double = 6.0

  /** Releases inside this, in tiles. */
  val behindExitTiles: Double = 4.0

  def shouldCatchUp(behindTiles: Double): Boolean =
    behindTiles > behindMarginTiles

  private val catchingUp = new Latch

  def reset(): Unit = catchingUp.reset()

  def catchUp(unitId: Int, behindTiles: Double): Boolean =
    catchingUp(
      unitId,
      fires    = behindTiles > behindEnterTiles,
      releases = behindTiles < behindExitTiles,
      initial  = shouldCatchUp(behindTiles))
}
