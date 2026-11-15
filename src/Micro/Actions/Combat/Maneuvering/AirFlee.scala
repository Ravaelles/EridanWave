package Micro.Actions.Combat.Maneuvering

import Mathematics.Maff
import Mathematics.Points.Pixel

/**
 * Air flee from anything that can hit us (no game access): flyers run from
 * the centroid of every live enemy that can attack them instead of standing
 * inside firing range, on a wound-scaled margin - a fresh hull skates, a
 * dying one turns early. The band is the caller's: Corsairs and Shuttles
 * skate at 3->10, eyes hold wider at 6->12. The flee destination never
 * lands in a map corner: corner squares are unusable traps, so a
 * destination inside one slides out along the nearest edge instead of
 * diving deeper. The trap width is the caller's too (Shuttles keep clear
 * of corners early, eyes only dodge the innermost squares). Pure pixels
 * in, pixels out; the caller gathers threats (any enemy that can attack
 * the victim), passes map bounds, and issues the order. One home for the
 * rule so air doctrines cannot drift apart on where safety ends.
 */
object AirFlee {

  /** Only enemies inside this radius, in tiles, trigger the flee. */
  val triggerTiles: Double = 12.0

  /** Safe margin, in tiles, for a fresh hull. */
  val marginLowTiles: Double = 3.0

  /** Safe margin, in tiles, for a dying hull. */
  val marginHighTiles: Double = 10.0

  /** Corner squares this wide, in tiles, are unusable: never flee into them. */
  val cornerTiles: Double = 6.0

  def marginTiles(wound01: Double, lowTiles: Double, highTiles: Double): Double =
    Maff.woundBonus(wound01, lowTiles, highTiles)

  /** Centroid of the threats, single or many: one answer, no convergence split. */
  def focus(threats: Seq[Pixel]): Option[Pixel] =
    if (threats.isEmpty) None else Some(Maff.centroid(threats))

  /** Raw flee point, past the margin on the own-focus line. */
  def fleeFrom(own: Pixel, focus: Pixel, marginTiles: Double): Pixel =
    own.project(focus, -marginTiles * 32)

  /**
   * Corner guard (pure map math): a destination inside a corner square
   * slides out along the nearest edge - vertically or horizontally off
   * the trap - instead of diving deeper. Destinations outside the map
   * clamp to its bounds first. Edges and center pass through untouched.
   * The trap width rides along so callers with longer flee legs keep
   * clear earlier; the default is the innermost squares.
   */
  def outOfCorner(dest: Pixel, mapWpx: Int, mapHpx: Int, cornerTiles: Double = AirFlee.cornerTiles): Pixel = {
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

  /**
   * Flee destination, if any threat is inside the trigger: centroid,
   * wound-scaled margin (custom bounds for eyes that never shoot back),
   * corner-guarded. Nothing in range means nothing.
   */
  def fleePixel(own: Pixel, threats: Seq[Pixel], wound01: Double, mapWpx: Int, mapHpx: Int): Option[Pixel] =
    fleePixel(own, threats, wound01, mapWpx, mapHpx, marginLowTiles, marginHighTiles)

  def fleePixel(
    own: Pixel,
    threats: Seq[Pixel],
    wound01: Double,
    mapWpx: Int,
    mapHpx: Int,
    marginLowTiles: Double,
    marginHighTiles: Double,
    triggerTiles: Double = AirFlee.triggerTiles): Option[Pixel] = {
    val near = threats.filter(t => own.pixelDistance(t) <= triggerTiles * 32.0)
    focus(near).map(focus =>
      outOfCorner(fleeFrom(own, focus, marginTiles(wound01, marginLowTiles, marginHighTiles)), mapWpx, mapHpx))
  }
}
