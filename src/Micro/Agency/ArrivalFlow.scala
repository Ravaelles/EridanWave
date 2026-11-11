package Micro.Agency

/**
 * Pure arrival-flow gate (no game access): a unit braking into a waypoint
 * crawls between hops and idles on arrival. Inside 1.8 tiles of its move
 * target, while the final destination lies beyond along the same heading,
 * push the target past its point so the unit flows through instead. True
 * arrivals (final at hand) and deliberate backsteps (target away from the
 * final) brake normally. Distances in tiles (rule 10); pixels convert once.
 * Knobs are public vars, KiteAway style, for tuning.
 */
object ArrivalFlow {

  /** Inside this radius of the move target the brake begins. */
  var triggerTiles: Double = 1.8

  /** Push the move target this far past its point while flowing. */
  var overshootTiles: Double = 1.8

  def pushPast(distToTargetTiles: Double, distToFinalTiles: Double, aligned: Boolean): Double =
    if (aligned && distToTargetTiles < triggerTiles && distToFinalTiles > distToTargetTiles) overshootTiles else 0.0
}
