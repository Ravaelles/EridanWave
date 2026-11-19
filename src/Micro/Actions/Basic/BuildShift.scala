package Micro.Actions.Basic

/**
 * Blocked-base shift (pure numbers for the suite): a builder told to
 * plant a town hall on a tile held by what it cannot see or kill - a
 * burrowed Zergling, a Spider Mine the probe is not allowed to duel -
 * waits on the spot forever instead of expanding. Count the stalled
 * frames and walk the placement outward instead: the spiral below
 * re-seats the base up to five tiles out, nearest first, so the colony
 * lands instead of donating the timing. A remembered unseen blocker
 * skips the wait entirely - the failure counter only covers blockers
 * nobody ever saw.
 */
object BuildShift {

  /** Re-seat the base this far out at most, in tiles. */
  val shiftRadiusTiles: Int = 5

  /** Stalled frames before the placement walks on blind. */
  val shiftAfterFrames: Int = 72

  /**
   * Placement index into the spiral (pure ints for the suite): the
   * original tile while fresh, then one step out per stalled window -
   * a remembered blocker jumps straight past the doomed tile.
   */
  def shiftIndex(failedAttempts: Int, rememberedBlocker: Boolean): Int =
    if (rememberedBlocker) 1 else failedAttempts / shiftAfterFrames
}
