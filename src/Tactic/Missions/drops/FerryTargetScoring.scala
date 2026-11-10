package Tactic.Missions.drops

/**
 * Pure ferry target scoring (no game access), Stardust ShuttleHarass as a
 * kernel: hunt tanks, sieged first, damaged first. Vetoes: recently
 * repaired tanks (back at full health, covered by the hp term instead),
 * air threat over a distant target, heavy non-static ground threat.
 * Anything vetoed scores None; the mission falls back to the front.
 */
object FerryTargetScoring {

  /** Unsieged tanks count extra distance: finish the deployed ones first. */
  val unsiegedPenalty: Double = 1.5

  /** Below this hp fraction a tank scores as nearly dead either way. */
  val minHpFraction: Double = 0.25

  /** Air threat veto applies past this distance: close drops risk it. */
  val airThreatRangePixels: Double = 48.0

  /** Non-static ground threat above this vetoes the drop point. */
  val groundThreatLimit: Double = 30.0

  def score(
    distPixels    : Double,
    sieged        : Boolean,
    hpFraction    : Double,
    airThreat     : Boolean,
    groundThreat  : Double): Option[Double] = {

    if (airThreat && distPixels > airThreatRangePixels) return None

    if (groundThreat > groundThreatLimit) return None

    Some(distPixels * (if (sieged) 1.0 else unsiegedPenalty) / Math.max(hpFraction, minHpFraction))
  }
}
