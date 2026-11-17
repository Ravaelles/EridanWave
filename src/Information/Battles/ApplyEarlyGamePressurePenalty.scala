package Information.Battles

/**
 * Early-game pressure penalty (no game access): a small Protoss-mirror
 * force facing enemy Dragoons fights worse than its hulls suggest -
 * unupgraded Dragoons trade evenly, so the short line breaks first. Any
 * retreat comparison multiplies our strength by the penalty while we
 * field fewer than seven combat units against their Dragoons; at parity
 * or with a grown army the score reads whole again.
 */
object ApplyEarlyGamePressurePenalty {

  /** Strength multiplier of a small force under Dragoon pressure. */
  val pressureFactor: Double = 0.8

  /** Below this many combat units the mirror outguns us per-hull. */
  val smallArmyUnits: Int = 7

  /**
   * Penalized strength (pure numbers for the suite): the caller passes
   * its own "ours" strength and combat-unit count with the enemy facts.
   */
  def adjust(oursStrength: Double, oursUnits: Int, enemyIsProtoss: Boolean, enemyHasDragoons: Boolean): Double =
    if (enemyIsProtoss && enemyHasDragoons && oursUnits < smallArmyUnits) oursStrength * pressureFactor
    else oursStrength
}
