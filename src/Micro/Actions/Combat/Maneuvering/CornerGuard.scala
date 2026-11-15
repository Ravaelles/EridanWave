package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel

/**
 * Corner guard (anti-stuck map geometry, no game access): corner squares
 * are unusable traps for a fleeing flyer - a destination inside one slides
 * out along the nearest edge instead of diving deeper, and destinations
 * outside the map clamp to its bounds first. Edges and the center pass
 * through untouched. Pure pixels in, pixels out; the trap width is the
 * caller's (flee legs differ, so Corsairs dodge the innermost squares
 * while Shuttles keep clear earlier). Split out of AirFlee: where safety
 * ends from enemies and where the map itself is unusable change for
 * different reasons. One home for the trap shape so air doctrines cannot
 * drift apart on it.
 */
object CornerGuard {

  /**
   * Corner guard: a destination inside the corner square slides out along
   * the shallower penetration - vertically or horizontally off the trap.
   */
  def guard(dest: Pixel, mapWpx: Int, mapHpx: Int, cornerTiles: Double): Pixel = {
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
