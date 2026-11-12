package Micro.Actions.Protoss.DarkTemplar

import Mathematics.Maff

/**
 * Pure detector berth for a hunting Dark Templar (no game access): a fresh
 * Templar skates the detector edge, a dying one gives mobile detectors
 * twice the room it used to - eight tiles at full wounds. A revealed
 * wounded Templar dies, so the berth grows with injury instead of staying
 * flat. Distances in tiles.
 */
object DarkTemplarBerth {

  /** Extra berth from mobile detectors at full wounds, in tiles. */
  val maxExtraTiles: Double = 8.0

  def extraTiles(wound01: Double): Double =
    maxExtraTiles * Maff.clamp(wound01, 0.0, 1.0)
}
