package Micro.Actions.Protoss.ReaverShuttle

import Mathematics.WoundMargin

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
    WoundMargin(baseMarginTiles, baseMarginTiles + woundMarginTiles).tiles(wound01)

  def shouldLand(distTiles: Double, attackedAtLanding: Boolean, threatDistTiles: Double, wound01: Double): Boolean =
    distTiles < engageTiles && ! attackedAtLanding && threatDistTiles > marginTiles(wound01)

  /**
   * Standoff vs statics we outrange (no game access, tiles): an escort
   * melee drags the landing inside the static's reach, and sitting there
   * trades hull for nothing - a live game watched a fresh Reaver take six
   * Sunken hits at 6.2 tiles. Back out to our max reach when we are inside
   * theirs and our gun is the longer one; outranged guns disengage by the
   * generic fighter instead of holding under them.
   */
  def advantageBackoff(ourRangeTiles: Double, staticRangeTiles: Double, distTiles: Double): Boolean =
    ourRangeTiles > staticRangeTiles && distTiles <= staticRangeTiles

  /**
   * Hard floor vs any defensive static (no game access, tiles): inside
   * this the hull steps out, no matter the order. Guns outrange us or
   * not, trading inside their reach mills the hull - a live game walked
   * a fresh Reaver straight into a Sunken on a WalkArmy order without
   * firing a shot. The caller scales the base with its own wound band
   * (a fresh Reaver holds 7.05, a holed shield backs out earlier); the
   * step is one frame of breathing room, not a full retreat, so a hull
   * holding its gun line outside the band never trips it.
   */
  val keepOffTiles: Double = 7.05

  /** Nudge step out of the floor, in tiles. */
  val stepTiles: Double = 0.5

  def shouldStepOut(distTiles: Double, floorTiles: Double): Boolean =
    distTiles <= floorTiles
}
