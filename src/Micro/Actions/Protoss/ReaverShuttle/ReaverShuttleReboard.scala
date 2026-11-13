package Micro.Actions.Protoss.ReaverShuttle

import Mathematics.Maff

/**
 * Reboard gate (no game access): a landed Reaver stays grounded and
 * shoots - boarding first is the circle that never deals damage.
 * Reboard only under direct threat: an enemy inside the wound-scaled
 * band means the position is lost and the ride is the way out.
 */
object ReaverShuttleReboard {

  /** Reboard band at full wounds, in tiles (woundBonus(0, 8)). */
  val woundFleeTiles: Double = 8.0

  def mayReboard(threatDistTiles: Double, wound01: Double): Boolean =
    threatDistTiles < Maff.woundBonus(wound01, 0.0, woundFleeTiles)
}
