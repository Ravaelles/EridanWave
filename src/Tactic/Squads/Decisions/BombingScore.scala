package Tactic.Squads.Decisions

/**
 * Pure bombing-target score (no game access): prey value, discounted by
 * anti-air cover and distance. Higher wins.
 */
object BombingScore {

  def score(preyValue: Double, aaValue: Double, distancePixels: Double): Double =
    preyValue / (1 + aaValue) / (640 + distancePixels)
}
