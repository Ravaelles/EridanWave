package Micro.Actions.Protoss.ReaverShuttle

import Mathematics.WoundMargin

/**
 * Reboard gate (no game access): a landed Reaver stays grounded and
 * shoots - boarding first is the circle that never deals damage.
 * Reboard only under direct threat: an enemy inside the wound-scaled
 * band means the position is lost and the ride is the way out. The band
 * never closes fully: a fresh hull in melee contact (a live UMS game
 * watched one stand at full health next to its ride and die) boards too.
 */
object ReaverShuttleReboard {

  /**
   * Scarab rescue (no game access): a flying Scarab cannot be spent twice -
   * board when pressured mid-shot, so the hull is safe while the shot lands.
   * Ported from the old discipline's first claim; without it a fresh hull
   * stands under guns with a Scarab in the air that boarding would save.
   */
  def rescueScarab(
    loaded: Boolean,
    committed: Boolean,
    hasActiveScarab: Boolean,
    cooldownLeft: Int,
    latencyFrames: Int,
    threatened: Boolean): Boolean =
    ! loaded &&
      ! committed &&
      hasActiveScarab &&
      cooldownLeft <= latencyFrames &&
      threatened

  /** Reboard band at full health, in tiles: melee contact always escapes. */
  val baseBoardTiles: Double = 2.0

  /** Reboard band at full wounds, in tiles (woundBonus(2, 8)). */
  val woundFleeTiles: Double = 8.0

  def mayReboard(threatDistTiles: Double, wound01: Double): Boolean =
    threatDistTiles < WoundMargin(baseBoardTiles, woundFleeTiles).tiles(wound01)

  /**
   * Rejoin reach, in tiles: a pressured hull walks to a ride this close
   * instead of standing under fire while it hovers just past the
   * pickup radius. The escort hovers at the same order of distance, so
   * the pickup completes on the way in instead of milling at the edge.
   */
  val rejoinTiles: Double = 5.0

  def rideInReach(distTiles: Double): Boolean =
    distTiles <= rejoinTiles
}
