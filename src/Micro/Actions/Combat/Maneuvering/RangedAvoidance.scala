package Micro.Actions.Combat.Maneuvering

import Mathematics.Maff
import Mathematics.Points.Pixel

/**
 * Generic standoff from enemy ranged units (no game access): flyers keep
 * out of the reach of anything with a real weapon instead of standing
 * inside firing range. Only threats inside the trigger count; one threat
 * runs straight away from it, many run from their centroid. The safe
 * margin is wound-scaled - a fresh hull skates, a dying one turns early.
 * Meant to be inherited: doctrines keep a configured instance with their
 * own knobs and call fleePixel, the caller gathers threats and issues
 * the order. Pure pixels in, pixels out.
 */
class RangedAvoidance(
  val triggerTiles: Double = RangedAvoidance.triggerTiles,
  val marginLowTiles: Double = RangedAvoidance.marginLowTiles,
  val marginHighTiles: Double = RangedAvoidance.marginHighTiles) {

  /** Flee destination, if any ranged threat is inside the trigger. */
  def fleePixel(own: Pixel, threats: Seq[Pixel], wound01: Double): Option[Pixel] = {
    val near = threats.filter(t => own.pixelDistance(t) <= triggerTiles * 32.0)
    RangedAvoidance.focus(near).map(focus =>
      RangedAvoidance.fleeFrom(own, focus, Maff.woundBonus(wound01, marginLowTiles, marginHighTiles)))
  }
}

object RangedAvoidance {

  /** Only enemies inside this radius, in tiles, trigger the flee. */
  val triggerTiles: Double = 12.0

  /** Safe margin, in tiles, for a fresh hull. */
  val marginLowTiles: Double = 3.0

  /** Safe margin, in tiles, for a dying hull. */
  val marginHighTiles: Double = 10.0

  def focus(threats: Seq[Pixel]): Option[Pixel] = {
    if (threats.isEmpty) None
    else if (threats.size == 1) Some(threats.head)
    else Some(Maff.centroid(threats))
  }

  /**
   * Raw flee point, past the margin on the own-focus line: no walkability
   * clamp here (that reads game grids, which suites do not have), the
   * caller clamps before issuing the order.
   */
  def fleeFrom(own: Pixel, focus: Pixel, marginTiles: Double): Pixel =
    own.project(focus, -marginTiles * 32)
}
