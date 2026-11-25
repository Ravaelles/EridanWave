package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel

/**
 * Corner guard (anti-stuck map geometry, no game access): corner squares
 * are unusable traps for a fleeing flyer - a destination inside one slides
 * out along the nearest edge instead of diving deeper, and destinations
 * outside the map clamp to its bounds first. Edges and the center pass
 * through untouched. Pure pixels in, pixels out; the trap widths live in
 * CornerAntiStuckGuard next to the shape they size, so every air doctrine
 * is tuned in one place. Split out of AirFlee: where safety ends from
 * enemies and where the map itself is unusable change for different
 * reasons. One home for the trap so air doctrines cannot drift apart
 * on it.
 */
object CornerAntiStuckGuard {

  /** Default trap width for air units, in tiles: never flee into it. */
  val defaultTiles: Double = 10.0

  /** Shuttle trap width, in tiles: 4->13-tile flee legs keep clear earlier. */
  val shuttleTiles: Double = 15.0

  /**
   * Corsair trap width, in tiles: inside it the escape pushes off the
   * corner as well as the enemies - a destination at the trap's edge
   * hugs the corner while the guns close in, so the zone reaches out
   * to twenty instead of guarding the innermost squares only.
   */
  val corsairTiles: Double = 20.0

  /** Enemy weight over corner weight when blending the escape inside the zone. */
  val enemyWeight: Double = 2.0

  /** Corner weight: the escape bends off, never against, the guns. */
  val cornerWeight: Double = 1.0

  /** Nearest map corner, by quadrant: the corner the escape bends off. */
  def nearestCorner(own: Pixel, mapWpx: Int, mapHpx: Int): Pixel =
    Pixel(if (own.x * 2 < mapWpx) 0 else mapWpx, if (own.y * 2 < mapHpx) 0 else mapHpx)

  /** Inside the zone the escape blends the corner in, never just the guns. */
  def inCornerZone(own: Pixel, mapWpx: Int, mapHpx: Int, zoneTiles: Double): Boolean =
    own.pixelDistance(nearestCorner(own, mapWpx, mapHpx)) <= zoneTiles * 32.0

  /**
   * Blended escape: the enemy leg (away-ray at full leg) plus the corner
   * leg (away-ray at the same leg), averaged two-to-one for the guns -
   * direction comes from both, distance stays one leg. Settles: aligned
   * legs project back onto the plain flee point instead of oscillating.
   */
  def blendedEscape(own: Pixel, focus: Pixel, corner: Pixel, legPixels: Double): Pixel = {
    val enemyLeg = own.project(focus, -legPixels)
    val cornerLeg = own.project(corner, -legPixels)
    val total = enemyWeight + cornerWeight
    Pixel(
      own.x + (((enemyLeg.x - own.x) * enemyWeight + (cornerLeg.x - own.x) * cornerWeight) / total).toInt,
      own.y + (((enemyLeg.y - own.y) * enemyWeight + (cornerLeg.y - own.y) * cornerWeight) / total).toInt)
  }

  /**
   * Corner guard: a destination inside the corner square slides out along
   * the shallower penetration - vertically or horizontally off the trap.
   */
  def guard(dest: Pixel, mapWpx: Int, mapHpx: Int, cornerTiles: Double = defaultTiles): Pixel = {
    val c = (cornerTiles * 32).toInt
    val x = Math.max(0, Math.min(mapWpx, dest.x))
    val y = Math.max(0, Math.min(mapHpx, dest.y))
    val inLeft = x < c
    val inRight = x > mapWpx - c
    val inTop = y < c
    val inBottom = y > mapHpx - c
    if ((inLeft || inRight) && (inTop || inBottom)) {
      val penX = Math.min(x, mapWpx - x)
      val penY = Math.min(y, mapHpx - y)
      if (penX < penY) Pixel(if (x * 2 < mapWpx) c else mapWpx - c, y)
      else Pixel(x, if (y * 2 < mapHpx) c else mapHpx - c)
    } else Pixel(x, y)
  }
}
