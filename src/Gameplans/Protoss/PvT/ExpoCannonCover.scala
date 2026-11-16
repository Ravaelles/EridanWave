package Gameplans.Protoss.PvT

/**
 * Expo cannon cover (pure numbers, no game access): weak against Terran
 * means every expansion holds at least one Cannon. Weak is mobile army
 * strength at or below 300 - static turtle score fakes the punch, so
 * the mobile-only read decides. One Cannon per expo rides the hall-side
 * template spots (DefendHall, between hall and minerals); the gameplans'
 * fixed counts still apply on top. The caller reads the race once and
 * the strength once; each expo owns its Cannon.
 */
object ExpoCannonCover {

  /** Cover expos while mobile army strength stays at or below this. */
  val weakArmyUnits: Double = 300.0

  /** Cannons per expansion while weak. */
  val cannonsPerExpo: Int = 1

  def shouldCover(enemyIsTerran: Boolean, armyStrengthUnits: Double): Boolean =
    enemyIsTerran && armyStrengthUnits <= weakArmyUnits
}
