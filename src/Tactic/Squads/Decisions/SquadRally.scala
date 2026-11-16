package Tactic.Squads.Decisions

import Mathematics.Points.Pixel
import Utilities.?
import Utilities.Latch

/**
 * Straggler rally (Stardust UnitCluster.move): units lagging far behind the
 * pack do not walk to their own destination - they rejoin the pack centroid
 * first instead of travelling alone.
 *
 * Slow hulls hold the line from further back: a Reaver keeps its own slot
 * (and its shot) instead of chasing the pack until five tiles past the
 * rally radius - being behind is fine, never shooting is not.
 *
 * Pure decision so it stays unit-testable without a game: given the unit's
 * position, the pack centroid and the terminus it would otherwise take,
 * answer which pixel it should head for.
 */
object SquadRally {

  /** Stragglers beyond this distance from the centroid rejoin instead of travelling. Mirrors Stardust's 128px rule. */
  val rallyRadiusPixels: Int = 128

  /** Extra leash for slow hulls (Reavers), in tiles: they walk their slot instead of chasing the pack inside it. */
  val slowUnitBonusTiles: Double = 5.0

  /** March leash in pixels: the rally radius, plus the slow-hull bonus where asked. */
  def rallyRadiusFor(slow: Boolean): Double =
    rallyRadiusPixels + ?(slow, slowUnitBonusTiles * 32, 0.0)

  def rallyFor(unitPixel: Pixel, centroid: Pixel, terminus: Pixel): Pixel = {
    rallyFor(unitPixel, centroid, terminus, rallyRadiusPixels)
  }

  def rallyFor(unitPixel: Pixel, centroid: Pixel, terminus: Pixel, radiusPixels: Double): Pixel = {
    if (isRallying(unitPixel, centroid, radiusPixels)) centroid else terminus
  }

  def isRallying(unitPixel: Pixel, centroid: Pixel): Boolean =
    isRallying(unitPixel, centroid, rallyRadiusPixels)

  def isRallying(unitPixel: Pixel, centroid: Pixel, radiusPixels: Double): Boolean = {
    unitPixel.pixelDistance(centroid) > radiusPixels
  }

  /** Rally hysteresis band, in tiles: enter past the radius, release a tile inside it. */
  val rallyBandTiles: Double = 1.0

  private val rallying = new Utilities.Latch

  /**
   * Latched rally (per-unit state, same band shape as the LoneMarch
   * latch): a straggler breathing on the rally radius holds its side
   * instead of flip-flopping between rejoining and marching every time
   * the pack centroid breathes. The pure gate above stays for callers
   * that decide, not hold.
   */
  def isRallyingLatched(unitId: Int, distTiles: Double, radiusTiles: Double): Boolean =
    rallying(
      unitId,
      fires    = distTiles > radiusTiles + rallyBandTiles / 2,
      releases = distTiles < radiusTiles - rallyBandTiles / 2,
      initial  = distTiles > radiusTiles)

  /** Marching units this far from their nearest ally close ranks instead of milling. */
  val allyJoinTiles: Double = 0.6

  def shouldJoinAlly(distTiles: Double): Boolean =
    distTiles >= allyJoinTiles

  /**
   * Retreat never rallies (pure flags for the suite): a warrior falling
   * back walks its own way out instead of rejoining the pack centroid -
   * rallying into the line it just left feeds the guns it fled. Non-warriors
   * keep their legs: probes and shuttles do not fight, so no verdict gates them.
   */
  def mayRally(isWarrior: Boolean, wantsFight: Boolean): Boolean =
    ! isWarrior || wantsFight
}
