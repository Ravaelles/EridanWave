package Micro.Actions.Protoss.DarkTemplar

import Micro.Actions.Combat.Maneuvering.RangedReach

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

  /**
   * Lone-detector radius, in tiles: with full shields and no attacker
   * this close, the reveal costs nothing yet - running just wastes
   * the hunt.
   */
  val holdVsDetectorTiles: Double = 10.0

  /** Hold-the-hide band beyond a ranged threat's own reach, in tiles. */
  val hideBandTiles: Double = 2.0

  /** Pixels below which a detector owns the Templar's position. */
  def evadeRadiusPixels(sightPixels: Double): Double =
    sightPixels + avoidBandTiles * 32.0

  def shouldEvade(distPixels: Double, sightPixels: Double): Boolean =
    distPixels < evadeRadiusPixels(sightPixels)

  def isRanged(rangePixels: Double): Boolean =
    RangedReach.isRanged(rangePixels)

  /**
   * Lone-detector hold (pure flags for the suite): full shields plus no
   * attacker near means the detector cannot punish the reveal, so the
   * hunt holds instead of running from an Overlord that cannot hit it.
   */
  def shouldHoldVsDetector(shieldsDown: Int, attackerNear: Boolean): Boolean =
    shieldsDown == 0 && ! attackerNear

  /**
   * Revealed-hide hold (pure numbers for the suite): a shot Templar is a
   * seen Templar, so while any ranged threat's reach plus the hide band
   * touches it, the hunt stays off - no re-attack into the guns that
   * just found it.
   */
  def shouldHide(distPixels: Double, rangePixels: Double): Boolean =
    isRanged(rangePixels) && distPixels < rangePixels + hideBandTiles * 32.0
}
