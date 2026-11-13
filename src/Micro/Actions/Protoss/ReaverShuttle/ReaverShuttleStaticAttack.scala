package Micro.Actions.Protoss.ReaverShuttle

import Mathematics.Maff

/**
 * Static-attack gate vs Zerg (no game access): inside twelve tiles of a
 * defensive static the pair lands and kills it on the ground instead of
 * circling - unless it was hit on the way in, or a threat already stands
 * inside the wound-scaled safety margin. The caller feeds distances and
 * the landing contact; this only combines them.
 */
object ReaverShuttleStaticAttack {

  /** Land on statics this close, in tiles. */
  val engageTiles: Double = 12.0

  /** Safe band past the static, in tiles, before wounds. */
  val baseMarginTiles: Double = 4.0

  /** Extra band at full wounds, in tiles (woundBonus(0, 6)). */
  val woundMarginTiles: Double = 6.0

  def marginTiles(wound01: Double): Double =
    baseMarginTiles + Maff.woundBonus(wound01, 0.0, woundMarginTiles)

  def shouldLand(distTiles: Double, attackedAtLanding: Boolean, threatDistTiles: Double, wound01: Double): Boolean =
    distTiles < engageTiles && ! attackedAtLanding && threatDistTiles > marginTiles(wound01)
}
