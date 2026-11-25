package Micro.Actions.Combat.Maneuvering

import Mathematics.Maff
import Mathematics.Points.Pixel

/**
 * Pure resource-clearance rule for fleeing (no game access): large land
 * units wedge between fields and die there, so flee spots keep minerals
 * 1.2 tiles off and geysers 3.1 - a hug against the geyser designates a
 * dead spot, while the wide ring strides past it to the far side, which
 * is where the duel wanted to go. Workers read as small and keep mining;
 * fliers do not care. Distances in tiles for the knobs, pixels at the
 * seam (the ring quantizes to whole pixels, so a pushed spot re-reads
 * as its own fixed point instead of flapping).
 */
object FleeMineralClearance {

  /** Flee spots keep this clearance from minerals, in tiles. */
  val mineralClearTiles: Double = 1.2

  /** Flee spots keep this clearance from geysers, in tiles. */
  val geyserClearTiles: Double = 3.1

  def applies(isLarge: Boolean, flying: Boolean): Boolean =
    isLarge && ! flying

  def clearOfResources(spot: Pixel, minerals: Seq[Pixel], geysers: Seq[Pixel]): Boolean =
    ! minerals.exists(m => spot.pixelDistance(m) < mineralClearTiles * 32.0) &&
    ! geysers.exists(g => spot.pixelDistance(g) < geyserClearTiles * 32.0)

  /**
   * Slide the spot off every violated ring along the away-ray (resource
   * to spot): the duel's direction survives, only the distance grows.
   * Bounded (three passes over a handful of pixels): a settled spot
   * projects onto itself instead of oscillating, and a wedge between
   * two fields still terminates instead of looping.
   */
  def pushClear(spot: Pixel, minerals: Seq[Pixel], geysers: Seq[Pixel]): Pixel = {
    var out = spot
    var guard = 0
    while (guard < 3 && ! clearOfResources(out, minerals, geysers)) {
      val rings = minerals.map(_ -> mineralClearTiles) ++ geysers.map(_ -> geyserClearTiles)
      Maff.minBy(rings)(ring => out.pixelDistance(ring._1)).foreach { case (center, ring) =>
        out = center.project(out, ring * 32.0)
      }
      guard += 1
    }

    out
  }
}
