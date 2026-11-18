package Micro.Actions.Protoss.Detector

import Mathematics.Points.Pixel
import Utilities.Latch

/**
 * Pure detector spread (no game access): Atlantis SpreadOutDetectors keeps
 * same-type eyes nine tiles apart, and only the newer unit moves - the
 * older holds its watch. Tiles in, tiles out; pixels only convert to call
 * the engine project (rule 12).
 */
object DetectorSpread {

  /** Same-type eyes keep at least this apart, in tiles (`Separation` carries the rule). */
  val minDistTiles: Double = 9.0

  /** Observers keep this clear of enemy detectors, in tiles. */
  val enemyKeepOffTiles: Double = 6.0

  /** Inside the keep-off radius the eye backs away instead of closing. */
  def shouldYieldToDetector(distTiles: Double): Boolean =
    distTiles < enemyKeepOffTiles

  /**
   * Science Vessels keep-off, in tiles: a Vessel sees eleven out, so an
   * eye inside eleven is already compromised - six would skim inside
   * the sight ring while the escort still pulls the eye in. Same
   * latched shape as the generic standoff, wider band.
   */
  val vesselKeepOffTiles: Double = 11.0

  /** Inside the Vessel ring the eye backs away instead of closing. */
  def shouldYieldToVessel(distTiles: Double): Boolean =
    distTiles < vesselKeepOffTiles

  /**
   * Standoff release margin, in tiles: a yielding eye keeps backing off
   * until the detector is this far past the keep-off radius. The escort
   * (monitor layer, every frame) pulls toward the pack while the brain
   * pushes off the detector, so a sharp edge alternates hundreds of
   * times - live games show StandOff/Escort flipping 1400-2200 times on
   * one eye until hunters catch it. Latched LoneMarch shape: enter sharp,
   * hold inside the band, release past the margin. Per-eye, reset per game.
   */
  val standOffReleaseTiles: Double = 2.0

  def standOffReleaseDistTiles: Double = enemyKeepOffTiles + standOffReleaseTiles

  private val yielding = new Latch

  def reset(): Unit = { yielding.reset(); yieldingVessel.reset() }

  def standOffLatched(eyeId: Int, distTiles: Double): Boolean =
    yielding(
      eyeId,
      fires    = shouldYieldToDetector(distTiles),
      releases = distTiles > standOffReleaseDistTiles,
      initial  = shouldYieldToDetector(distTiles))

  /** Full standoff from an enemy detector along the ray past us. */
  def standOff(us: Pixel, them: Pixel): Pixel =
    them.project(us, enemyKeepOffTiles * 32.0)

  /**
   * Vessel standoff release, in tiles: same latched shape as the generic
   * ring - enter sharp at eleven, hold inside the band, release past
   * thirteen - so the escort pull cannot flap the eye on the edge.
   */
  val vesselReleaseTiles: Double = 2.0

  def vesselReleaseDistTiles: Double = vesselKeepOffTiles + vesselReleaseTiles

  private val yieldingVessel = new Latch

  def standOffVesselLatched(eyeId: Int, distTiles: Double): Boolean =
    yieldingVessel(
      eyeId,
      fires    = shouldYieldToVessel(distTiles),
      releases = distTiles > vesselReleaseDistTiles,
      initial  = shouldYieldToVessel(distTiles))

  /** Full standoff from a Vessel along the ray past us. */
  def standOffVessel(us: Pixel, them: Pixel): Pixel =
    them.project(us, vesselKeepOffTiles * 32.0)

  /**
   * Detected-eye margin, in tiles: a compromised eye never sits inside
   * the guns - it holds this clear of the centroid of everything that
   * can hit it. Small on purpose: detection already costs the watch,
   * the margin only buys the walk-out instead of donating the hull.
   */
  val detectedMarginTiles: Double = 4.0

  /** Step past the margin, in tiles: the fallback lands outside, not on it. */
  val detectedPastLineTiles: Double = 1.0

  /** Inside the detected margin the eye steps out instead of holding. */
  def shouldYieldToCentroid(distTiles: Double): Boolean =
    distTiles < detectedMarginTiles

  /** Full step-out from the gun centroid along the ray past us. */
  def stepOutFromCentroid(us: Pixel, centroid: Pixel): Pixel =
    centroid.project(us, (detectedMarginTiles + detectedPastLineTiles) * 32.0)
}
