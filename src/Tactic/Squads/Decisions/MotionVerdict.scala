package Tactic.Squads.Decisions

/**
 * Motion verdict (pure mode order, no game access): one owner per unit
 * per frame. Withdraw outranks everything (a held retreat walks home,
 * never duels on the way out), contact outranks dressing (a lone hull
 * with a hittable duel or incoming fire holds the duel instead of
 * marching at the goal), dressing outranks the open march. Read by the
 * march, the technique and the formation from the same inputs both
 * phases already measure - the owners keep their math, the ownership
 * stops being renegotiated every frame. Stage 1 is observe-only: the
 * march logs desired-vs-actual through the Motion probe so the
 * alternation rate is measured before any behavior reads the verdict.
 */
object MotionVerdict {

  /** Who owns this unit's movement this frame. */
  sealed trait Mode {
    def label: String
  }

  /** Walk home: the held retreat verdict owns the frame. */
  case object Withdraw extends Mode {
    override def label: String = "Withdraw"
  }

  /** Duel: a hittable target or incoming fire owns the frame. */
  case object Duel extends Mode {
    override def label: String = "Duel"
  }

  /** Dress: the far-wait or the pack legs own the frame. */
  case object Dress extends Mode {
    override def label: String = "Dress"
  }

  /** Advance: nobody else claims the frame, walk at the goal. */
  case object Advance extends Mode {
    override def label: String = "Advance"
  }

  /**
   * Desired owner (pure flags for the suite): first claim wins, in the
   * documented order. No memory here - the latch lives with the reader
   * (stage 2), so the order itself stays a stateless function the suite
   * pins row by row.
   */
  def desired(verdictRetreat: Boolean, hasDuel: Boolean, dressing: Boolean): Mode =
    if (verdictRetreat) Withdraw
    else if (hasDuel) Duel
    else if (dressing) Dress
    else Advance
}
