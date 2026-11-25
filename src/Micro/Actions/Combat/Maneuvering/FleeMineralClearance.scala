package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel

/**
 * Pure resource-clearance rule for fleeing (no game access): large land
 * units wedge between fields and die there, so flee spots stay 1.2
 * tiles off minerals and geysers alike. Workers read as small and keep
 * mining; fliers do not care. Distances in tiles for the knob, pixels
 * at the seam (the ring quantizes to whole pixels, so a pushed spot
 * re-reads as its own fixed point instead of flapping).
 */
object FleeMineralClearance {

  /** Flee spots keep this clearance from minerals and geysers, in tiles. */
  val resourceClearTiles: Double = 1.2

  def applies(isLarge: Boolean, flying: Boolean): Boolean =
    isLarge && ! flying

  def violates(spot: Pixel, resourceCenter: Pixel): Boolean =
    spot.pixelDistance(resourceCenter) < resourceClearTiles * 32.0

  def clearOfResources(spot: Pixel, resourceCenters: Seq[Pixel]): Boolean =
    ! resourceCenters.exists(violates(spot, _))

  /**
   * Slide the spot onto the 1.2 ring along the away-ray (resource to
   * spot): the duel's direction survives, only the distance grows, and
   * a settled spot projects onto itself instead of oscillating.
   */
  def pushClear(spot: Pixel, resourceCenter: Pixel): Pixel =
    resourceCenter.project(spot, resourceClearTiles * 32.0)
}
