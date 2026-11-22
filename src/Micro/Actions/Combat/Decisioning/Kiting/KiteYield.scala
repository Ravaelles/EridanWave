package Micro.Actions.Combat.Decisioning.Kiting

/**
 * Kite-yield cap (owner direction: only yield what the cooldown forces):
 * the step out never exceeds what the hull walks back inside the
 * remaining cooldown, so the return trip stops eating the next volley.
 * Pure pixels and frames for the suite; the kite leg reads it beside
 * the wound want. A zero cap plants by construction - the hold credit
 * fires instead of a move to nowhere.
 */
object KiteYield {

  /**
   * Capped step: the wound want, or the walk-back inside the cooldown
   * after reaction, whichever is smaller. Reaction covers latency plus
   * the attack-delay margin, so the volley lands on time, not late.
   */
  def offsetPixels(woundWantPixels: Double, cooldownLeft: Int, topSpeedPixels: Double, reactionFrames: Int): Double =
    Math.min(woundWantPixels, Math.max(0, cooldownLeft - reactionFrames) * topSpeedPixels)
}
