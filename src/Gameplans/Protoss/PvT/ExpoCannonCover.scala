package Gameplans.Protoss.PvT

/**
 * Expo cannon cover (pure numbers, no game access): weak against Terran
 * means every expansion beyond main and natural holds at least one
 * Cannon (those two ride the gameplans' fixed counts). Weak is mobile army
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

  /** Vultures on the map mean raiders: this many sighted. */
  val vultureCount: Int = 4

  /** Cannons per expansion against vultures: one tight, one covering. */
  val vultureCannonsPerExpo: Int = 2

  def shouldCover(enemyIsTerran: Boolean, armyStrengthUnits: Double): Boolean =
    enemyIsTerran && armyStrengthUnits <= weakArmyUnits

  def shouldCoverVsVultures(enemyIsTerran: Boolean, vultures: Int): Boolean =
    enemyIsTerran && vultures >= vultureCount
}
