package Micro.Actions.Combat.Decisioning

/**
 * HoldLine advance (no game access): a dressed line that is holding does
 * not freeze - it walks its formation at the enemy in fixed micro-steps
 * instead of standing pawns or breaking into a charge. The step is a
 * tile fraction per frame on purpose: at full speed the arc trades
 * before it is ever used, standing still lets the enemy dictate the
 * range. Only a dressed line advances (a scattered line reforms
 * first), never melee (it must close anyway) and never past the target.
 * The step scales with the local odds: a stronger line walks bolder,
 * a weaker one inches - but it always walks, never waits.
 */
object HoldLineAdvance {

  /** Formation advance per frame while holding, in tiles. */
  val advanceTiles: Double = 0.1

  /** Dressed enough to advance as one line, in fraction of warriors in slot. */
  val advanceReadyFraction: Double = 0.8

  /** Advance courage: the odds band the step scales inside. */
  val minOddsShare: Double = 0.5
  val maxOddsShare: Double = 2.0

  def shouldAdvance(
    hasFormation: Boolean,
    melee: Boolean,
    weaponReady: Boolean,
    fractionInSlot: Double,
    beyondRangeTiles: Double): Boolean =
    hasFormation && ! melee && weaponReady && fractionInSlot >= advanceReadyFraction && beyondRangeTiles > 0.0

  /**
   * Step for the odds: our skim over theirs, clamped to the courage
   * band. Unknown odds (no measured enemy) walk the base step - a line
   * never sprints at ghosts nor freezes before them.
   */
  def advanceTilesFor(usSkim: Double, foeSkim: Double): Double =
    if (foeSkim <= 0) advanceTiles
    else advanceTiles * Math.min(maxOddsShare, Math.max(minOddsShare, usSkim / foeSkim))

  /** One step toward the target, never past it, in tiles. */
  def stepTiles(distTiles: Double, stepTiles: Double = advanceTiles): Double =
    Math.max(0.0, Math.min(distTiles, stepTiles))
}
