package Micro.Targeting

/**
 * Pure carrier tank-hunt doctrine (no game access): carriers inside tank
 * range always dive the tanks - focused interceptors kill faster than
 * sieged tanks track - unless the wing is toothless (under ten
 * interceptors combined) and real anti-air (two or more) waits, or the
 * wing is naked outright. Distances in tiles.
 */
object CarrierTankHunt {

  /** Carriers this close to a tank always go for it. */
  val tankHuntTiles: Double = 15.0

  /** At or above this many combined interceptors the wing dives regardless. */
  val minInterceptors: Int = 10

  /** This many enemy AA near the carrier vetoes a toothless dive. */
  val maxToleratedAA: Int = 1

  def shouldHunt(tankNear: Boolean, totalInterceptors: Int, enemyAANear: Int): Boolean =
    tankNear && (totalInterceptors >= minInterceptors
      || (totalInterceptors > 0 && enemyAANear <= maxToleratedAA))
}
