package Tactic.Squads.Decisions

import Mathematics.Points.Pixel

/**
 * Straggler rally (Stardust UnitCluster.move): units lagging far behind the
 * pack do not walk to their own destination - they rejoin the pack first.
 *
 * Pure decision so it stays unit-testable without a game: given the unit's
 * position, the pack centroid and the terminus it would otherwise take,
 * answer which pixel it should head for.
 */
object SquadRally {

  /** Stragglers beyond this distance from the centroid rejoin instead of travelling. Mirrors Stardust's 128px rule. */
  val rallyRadiusPixels: Int = 128

  def rallyFor(unitPixel: Pixel, centroid: Pixel, terminus: Pixel): Pixel = {
    if (isRallying(unitPixel, centroid)) centroid else terminus
  }

  def isRallying(unitPixel: Pixel, centroid: Pixel): Boolean = {
    unitPixel.pixelDistance(centroid) > rallyRadiusPixels
  }
}
