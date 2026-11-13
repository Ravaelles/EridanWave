package Micro.Actions.Combat.Maneuvering

import Mathematics.Maff
import Mathematics.Points.Pixel
import Utilities.?

/**
 * Pure air-unit flee geometry (no game access): flyers withdraw from
 * enemies that can hit them instead of standing inside firing range.
 * One enemy inside twelve tiles: run straight away from it. More:
 * run from their centroid. The safe margin grows linearly with wounds,
 * from a 2.5 tile base up to 10.5 bleeding - a fresh hull skates, a
 * dying one keeps its distance early. Pure pixels in, pixels out;
 * the caller gathers threats and issues the order.
 */
object AirFleeGeometry {

  /** Only enemies inside this radius, in tiles, trigger the flee. */
  val threatRadiusTiles: Double = 12.0

  /** A passenger this close, in tiles, still counts as fighting. Shorter
    * than the flee trigger on purpose: the Shuttle holds its errand while
    * shots land nearby and only runs once threats close the extra two. */
  val fightRadiusTiles: Double = 10.0

  /** Base safe margin, in tiles, for a fresh hull. */
  val baseMarginTiles: Double = 2.5

  /** Wound bonus, in tiles, linear in woundPercent: 0 fresh, 8 dying. */
  val woundBonusTiles: Double = 8.0

  def safeMarginTiles(woundPercent: Double): Double =
    Maff.woundBonus(woundPercent, baseMarginTiles, baseMarginTiles + woundBonusTiles)

  def focus(threats: Seq[Pixel]): Option[Pixel] = {
    if (threats.isEmpty) None
    else if (threats.size == 1) Some(threats.head)
    else Some(Maff.centroid(threats))
  }

  def fleePixel(own: Pixel, focus: Pixel, woundPercent: Double): Pixel = {
    val away = own.project(focus, -safeMarginTiles(woundPercent) * 32)
    ?(away.walkable, away, away.walkablePixel)
  }
}
