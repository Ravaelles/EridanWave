package Micro.Actions.Combat.Decisioning

/**
 * HoldLine creep (no game access): a dressed line that is holding does
 * not freeze - it walks its formation at the enemy in fixed micro-steps
 * instead of standing pawns or breaking into a charge. The step is a
 * tile fraction per frame on purpose: at full speed the arc trades
 * before it is ever used, standing still lets the enemy dictate the
 * range. Only a dressed line creeps (a scattered line reforms first),
 * never melee (it must close anyway) and never past the target.
 */
object HoldLineCreep {

  /** Formation advance per frame while holding, in tiles. */
  val creepTiles: Double = 0.1

  /** Dressed enough to creep as one line, in fraction of warriors in slot. */
  val creepReadyFraction: Double = 0.8

  def shouldCreep(
    hasFormation: Boolean,
    melee: Boolean,
    weaponReady: Boolean,
    fractionInSlot: Double,
    beyondRangeTiles: Double): Boolean =
    hasFormation && ! melee && weaponReady && fractionInSlot >= creepReadyFraction && beyondRangeTiles > 0.0

  /** One step toward the target, never past it, in tiles. */
  def stepTiles(distTiles: Double, stepTiles: Double = creepTiles): Double =
    Math.max(0.0, Math.min(distTiles, stepTiles))
}
