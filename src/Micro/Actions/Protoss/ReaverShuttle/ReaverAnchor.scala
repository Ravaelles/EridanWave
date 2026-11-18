package Micro.Actions.Protoss.ReaverShuttle

import Mathematics.Maff
import Mathematics.WoundMargin

/**
 * Reaver pack anchor (no game access): a slow hull never stands ahead
 * of its own pack - ahead of the centroid it is a free kill, behind it
 * it is a volley. Distances are ground tiles to our main, so the anchor
 * holds across chokes and ramps, not through them. The wound penalty
 * pulls a hurt hull further back: fresh hulls hold the centroid line,
 * dying ones keep up to four tiles closer to home.
 */
object ReaverAnchor {

  /** Extra fallback of a fully wounded hull, in tiles. */
  val woundPenaltyTiles: Double = 4.0

  /** Furthest ground-tiles-to-main the hull may hold: the pack line minus the wound penalty. */
  def maxAdvanceTiles(armyMainTiles: Double, wound01: Double): Double =
    armyMainTiles - WoundMargin(0.0, woundPenaltyTiles).tiles(wound01)

  /** True while the hull stands further from home than the pack allows. */
  def beyondAnchor(reaverMainTiles: Double, armyMainTiles: Double, wound01: Double): Boolean =
    reaverMainTiles > maxAdvanceTiles(armyMainTiles, wound01)

  def shouldBackToPack(reaverMainTiles: Double, armyMainTiles: Double, wound01: Double): Boolean =
    beyondAnchor(reaverMainTiles, armyMainTiles, Maff.clamp01(wound01))
}
