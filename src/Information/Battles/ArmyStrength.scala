package Information.Battles

/**
 * Pure global army strength (no game access): 100 means even, higher
 * means stronger. No scouted enemy counts as even. Displayed on the
 * CherryVis board - and below the retreat threshold it breaks off the
 * attack squad, whatever the local sims say.
 */
object ArmyStrength {

  def score(ours: Double, foe: Double): Double =
    if (foe <= 0.0) 100.0 else 100.0 * ours / foe

  /** Below this global score the attack squad breaks off, whatever the
    * local sims say: reinforcing a losing side feeds it piecemeal, and
    * fresh units march back out as the score recovers. */
  val retreatBelowStrength: Double = 130.0

  def retreating(ours: Double, foe: Double): Boolean =
    foe > 0.0 && score(ours, foe) < retreatBelowStrength
}
