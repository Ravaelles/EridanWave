package Micro.Targeting

/**
 * Pure carrier brawl doctrine (no game access): a wing this heavy never
 * retreats - sixty interceptors delete anything that stays to fight.
 * Airborne interceptors work a nine tile beat around each carrier: the
 * carrier holds its kill while it lasts inside the beat, else takes the
 * nearest enemy inside it, so the whole pack gets chewed instead of one
 * chased target. Distances in tiles.
 */
object CarrierBrawl {

  /** At or above this many combined interceptors the wing always fights. */
  val minInterceptors: Int = 50

  /** Airborne interceptors work everything inside this beat, in tiles. */
  val brawlRadiusTiles: Double = 9.0

  /** Enemies inside this leash, in tiles, keep their target lock. */
  val leashTiles: Double = 10.0

  /** A target beyond this beat, in tiles, is chased, not held. */
  val launchTiles: Double = 9.0

  /** A target inside this leash, in tiles, opens it: the Carrier backs off. */
  val openLeashTiles: Double = 8.0

  /** The hold-leash anchor sits this far home, in tiles. */
  val homeLeashTiles: Double = 7.5

  def shouldBrawl(totalInterceptors: Int): Boolean =
    totalInterceptors >= minInterceptors

  def inBrawl(distTiles: Double): Boolean =
    distTiles <= brawlRadiusTiles
}
