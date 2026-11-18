package Micro.Actions.Combat.Decisioning

/**
 * Storm stand (pure numbers, no game access): a Templar holding a live
 * storm does not retreat out of cast range. Below the approach band the
 * retreat stands; inside it - or while the squad fights - the hull holds
 * the line or walks in instead of trading the battery for distance. The
 * cast itself stays top priority: the Fight chain fires spells before
 * the technique moves, so a storm in reach lands on the same frame.
 * The stand is siege-gated, not race-gated: walking Marines down is
 * allowed danger, walking into siege shells is not - the cast itself
 * still vetoes tank-covered boxes, so the walk never buys a storm it
 * cannot land.
 */
object StormStand {

  /** Walk into cast range from this far out, in tiles. */
  val approachTiles: Double = 12.0

  def shouldHold(
    isTemplar: Boolean,
    enemyIsZerg: Boolean,
    energyReady: Boolean,
    nearestFoeTiles: Double,
    fightConsensus: Boolean,
    siegeThreat: Boolean): Boolean =
    isTemplar &&
      (enemyIsZerg || ! siegeThreat) &&
      energyReady &&
      (fightConsensus || nearestFoeTiles <= approachTiles)
}
