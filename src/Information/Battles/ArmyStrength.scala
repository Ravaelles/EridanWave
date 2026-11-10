package Information.Battles

/**
 * Pure global army strength (no game access): 100 means even, higher
 * means stronger. No scouted enemy counts as even. Display-only signal
 * for the CherryVis board - fight/withdraw decisions run on the finer
 * per-unit sim verdicts (see SquadAttack stance), never on this ratio.
 */
object ArmyStrength {

  def score(ours: Double, foe: Double): Double =
    if (foe <= 0.0) 100.0 else 100.0 * ours / foe
}
