package Tactic.Squads.Decisions

/**
 * Timed retreat hold (no game access): the raw verdict flaps with its
 * inputs (cooldowns, pushes, sims) and with scheduling skew between the
 * squad and unit phases (budget-driven task order, Planning skips), so
 * both readers obey a held verdict instead of the raw one. Entering is
 * sharp - a fresh retreat holds at once; releasing waits out
 * `holdFrames` past the last fight edge, so a verdict that flickers
 * fight for a frame or two cannot drag the march home and back. The
 * edge age lives with the caller (one verdict object shared by both
 * readers); this stays pure for the suite.
 */
object RetreatHold {

  /**
   * Held verdict (pure numbers for the suite): a raw retreat holds now;
   * a raw fight releases only after `holdFrames` consecutive fight
   * frames past the edge. Zero disables the hold (raw passes through).
   */
  def held(rawHold: Boolean, framesSinceFightEdge: Int, holdFrames: Int): Boolean =
    rawHold || framesSinceFightEdge < holdFrames
}
