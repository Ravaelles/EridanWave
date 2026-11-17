package Micro.Actions.Combat.Decisioning

/**
 * Re-engage latch (pure numbers, no game access): technique selection
 * recomputes from scratch every frame, so a verdict hovering at
 * fifty-fifty swings the unit between Approach and Retreat inside enemy
 * range until it dies there. Once retreating, the retreat holds until
 * the unit is clear of the deepest threat range with margin AND the
 * verdict says fight - withdrawing past the guns before turning is the
 * whole point - or the duel is confident enough to turn inside contact
 * anyway. Mirrors the squad-level posture deadband at unit scale.
 */
object ReengageLatch {

  /** Hold the retreat until this clear of the deepest threat, in tiles (== MatchupAnalysis.safetyMargin). */
  val clearTiles: Double = 5.0

  /** ...or this confident of winning the duel ahead, -1..1 (chasing a runner takes 0.25; turning inside contact wants more). */
  val confidentToReturn: Double = 0.5

  def shouldHoldRetreat(wasRetreating: Boolean, entangledTiles: Double, verdictFight: Boolean, confidence11: Double): Boolean =
    wasRetreating && !(verdictFight && (entangledTiles <= -clearTiles || confidence11 > confidentToReturn))
}
