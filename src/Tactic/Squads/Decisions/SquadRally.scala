package Tactic.Squads.Decisions

import Mathematics.Points.Pixel
import Utilities.?

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

  /** Marching units this far from their nearest ally close ranks instead of milling. */
  val allyJoinTiles: Double = 0.6

  def shouldJoinAlly(distTiles: Double): Boolean =
    distTiles >= allyJoinTiles
}
