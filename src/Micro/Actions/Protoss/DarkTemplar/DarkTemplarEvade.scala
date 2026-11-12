package Micro.Actions.Protoss.DarkTemplar

/**
 * Pure detector evasion for a hunting Dark Templar (no game access): a
 * detector that can see the Templar unmakes it, so the hunt never leads
 * through detection. Inside the detector's sight plus a two-tile avoidance
 * band the Templar breaks off everything - sniping, hunting, holding -
 * and runs; the caller steers directly away. Distances in pixels.
 */
object DarkTemplarEvade {

  /** Avoidance band outside the detector's sight that still evades, in tiles. */
  val avoidBandTiles: Double = 2.0

  /** Reach above which a threat counts as ranged, in pixels. */
  val rangedReachPixels: Double = 32.0

  /** Hold-the-hide band beyond a ranged threat's own reach, in tiles. */
  val hideBandTiles: Double = 2.0

  /** Pixels below which a detector owns the Templar's position. */
  def evadeRadiusPixels(sightPixels: Double): Double =
    sightPixels + avoidBandTiles * 32.0

  def shouldEvade(distPixels: Double, sightPixels: Double): Boolean =
    distPixels < evadeRadiusPixels(sightPixels)

  def isRanged(rangePixels: Double): Boolean =
    rangePixels > rangedReachPixels

  /**
   * Revealed-hide hold (pure numbers for the suite): a shot Templar is a
   * seen Templar, so while any ranged threat's reach plus the hide band
   * touches it, the hunt stays off - no re-attack into the guns that
   * just found it.
   */
  def shouldHide(distPixels: Double, rangePixels: Double): Boolean =
    isRanged(rangePixels) && distPixels < rangePixels + hideBandTiles * 32.0
}
