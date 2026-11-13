package Micro.Actions.Protoss.ReaverShuttle

import Mathematics.Maff

/**
 * Reboard gate (no game access): a landed Reaver stays grounded and
 * shoots - boarding first is the circle that never deals damage.
 * Reboard only under direct threat: an enemy inside the wound-scaled
 * band means the position is lost and the ride is the way out. The band
 * never closes fully: a fresh hull in melee contact (a live UMS game
 * watched one stand at full health next to its ride and die) boards too.
 */
object ReaverShuttleReboard {

  /** Reboard band at full health, in tiles: melee contact always escapes. */
  val baseBoardTiles: Double = 2.0

  /** Reboard band at full wounds, in tiles (woundBonus(2, 8)). */
  val woundFleeTiles: Double = 8.0

  def mayReboard(threatDistTiles: Double, wound01: Double): Boolean =
    threatDistTiles < Maff.woundBonus(wound01, baseBoardTiles, woundFleeTiles)
}
