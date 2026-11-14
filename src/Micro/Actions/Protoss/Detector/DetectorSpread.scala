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

  def reset(): Unit = { yielding.reset() }

  def standOffLatched(eyeId: Int, distTiles: Double): Boolean =
    yielding(
      eyeId,
      fires    = shouldYieldToDetector(distTiles),
      releases = distTiles > standOffReleaseDistTiles,
      initial  = shouldYieldToDetector(distTiles))

  /** Full standoff from an enemy detector along the ray past us. */
  def standOff(us: Pixel, them: Pixel): Pixel =
    them.project(us, enemyKeepOffTiles * 32.0)
}
