package Micro.Actions.Protoss.DarkTemplar

import Mathematics.Points.Pixel
import Micro.Actions.Combat.Maneuvering.RangedReach
import Utilities.Latch

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

  /**
   * Attacker-flee radius, in tiles: a visible enemy that can actually hit
   * the Templar this close outranks the detector - flee the guns, not
   * the eyes. Overlords and Observers cannot hit back, so the reveal
   * alone never triggers this; Marines and Hydras do.
   */
  val attackerFleeTiles: Double = 13.0

  def shouldFleeAttacker(distTiles: Double): Boolean =
    distTiles <= attackerFleeTiles

  /** Pixels below which a detector owns the Templar's position. */
  def evadeRadiusPixels(sightPixels: Double): Double =
    sightPixels + avoidBandTiles * 32.0

  def shouldEvade(distPixels: Double, sightPixels: Double): Boolean =
    distPixels < evadeRadiusPixels(sightPixels)

  /**
   * Exit margin beyond the avoidance band, in tiles: an evading Templar
   * keeps running until the detector is this far past the band. The hunt
   * destination (toward the enemy, often past the detector) directly
   * opposes the evade destination (away), so a sharp release re-triggers
   * on the next approach: live games show Hunt/Evade alternating ~150
   * times until the Templar dies without connecting. Latched LoneMarch
   * shape: enter sharp, hold inside the band, release past the margin.
   */
  val exitMarginTiles: Double = 2.0

  def evadeReleasePixels(sightPixels: Double): Double =
    evadeRadiusPixels(sightPixels) + exitMarginTiles * 32.0

  private val evading = new Latch

  def reset(): Unit = { evading.reset() }

  def evadeLatched(unitId: Int, distPixels: Double, sightPixels: Double): Boolean =
    evading(
      unitId,
      fires    = shouldEvade(distPixels, sightPixels),
      releases = distPixels > evadeReleasePixels(sightPixels),
      initial  = shouldEvade(distPixels, sightPixels))

  /**
   * Covered-target filter (pure pixels for the suite): a hunt target
   * standing inside a likely detector's evade radius re-arms the evade
   * on arrival, so the hunt skips it and holds instead of feeding the
   * same loop the latch just broke. The caller passes likely detectors
   * only; the band matches the evade above, so release and hunt agree.
   */
  def underDetection(target: Pixel, detectors: Seq[(Pixel, Double)]): Boolean =
    detectors.exists { case (detectorPixel, sightPixels) =>
      target.pixelDistance(detectorPixel) < evadeRadiusPixels(sightPixels)
    }

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
