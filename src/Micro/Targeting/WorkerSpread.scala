package Micro.Targeting

/**
 * Worker spread (no game access): a firing line queues on the weakest
 * worker while the rest of the mineral line walks away. Sort the hittable
 * workers by id for a frame-stable order, cap the draw at three, deal by
 * attacker id - every shooter holds its own worker instead of the whole
 * line chasing one. Deterministic per unit (no flap), spread across units.
 */
object WorkerSpread {

  /** Victims in one draw: the line spreads over at most this many. */
  val spreadDraw: Int = 3

  /**
   * Index into a stable (id-sorted) worker list for this attacker.
   * Pure numbers for the suite; the caller sorts, this deals.
   */
  def pickSlot(attackerId: Int, workers: Int): Int =
    if (workers <= 0) 0 else Math.abs(attackerId) % Math.min(spreadDraw, workers)
}
