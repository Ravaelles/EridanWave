package Tactic

/**
 * Forced global attack behind the unseen blade (no game access): with our
 * first Dark Templar alive in a Protoss or Terran macro game and no mobile
 * detection on the enemy (Observers, Vessels), the fire support decides
 * the game now - the army marches regardless of the score instead of
 * sitting home. The only things it does not chew are covered static
 * defense without advantage; free shots stay legal so outranging guns
 * still die for nothing.
 */
object ForceGlobalAttack {

  /** Units-strength score (ArmyStrength.score, 100 = parity) that buys static objectives. */
  val staticAttackScore: Double = 140.0

  /**
   * Force the attack: macro game, Protoss or Terran enemy, our Dark
   * Templar alive, no enemy mobile detection. Pure booleans for the
   * suite; the caller reads scouting and the tracker.
   */
  def shouldForce(
    enemyIsProtoss: Boolean,
    enemyIsTerran: Boolean,
    haveDarkTemplar: Boolean,
    enemyHasDetection: Boolean,
    macroGame: Boolean): Boolean =
    macroGame && (enemyIsProtoss || enemyIsTerran) && haveDarkTemplar && ! enemyHasDetection

  /**
   * Static legality under the force (no game access): covered statics
   * wait for advantage; everything else - units, workers, bases, free
   * shots inside our own range - stays legal.
   */
  def mayHitStatic(
    forceActive: Boolean,
    targetIsStaticDefense: Boolean,
    advantaged: Boolean,
    inOurRange: Boolean): Boolean =
    ! forceActive || ! targetIsStaticDefense || advantaged || inOurRange
}
