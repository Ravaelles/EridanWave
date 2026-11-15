package Micro.Actions.Combat.Maneuvering

import Mathematics.{Maff, WoundMargin}
import Mathematics.Points.Pixel

/**
 * Air flee from anything that can hit us (no game access): flyers run from
 * the centroid of every live enemy that can attack them instead of standing
 * inside firing range, on a wound-scaled margin - a fresh hull skates, a
 * dying one turns early. The band is the caller's: Corsairs and Shuttles
 * skate at 3->10, eyes hold wider at 6->12. The flee destination never
 * lands in a map corner: CornerAntiStuckGuard slides a destination out of
 * the trap squares instead of diving deeper. Pure pixels in, pixels out; the caller gathers threats (any enemy that can attack the victim),
 * passes map bounds, and issues the order. One home for the flee rule so
 * air doctrines cannot drift apart on where safety ends.
 */
object AirFlee {

  /** Only enemies inside this radius, in tiles, trigger the flee. */
  val triggerTiles: Double = 12.0

  /** Safe margin, in tiles, for a fresh hull. */
  val marginLowTiles: Double = 3.0

  /** Safe margin, in tiles, for a dying hull. */
  val marginHighTiles: Double = 10.0

  def marginTiles(wound01: Double, lowTiles: Double, highTiles: Double): Double =
    WoundMargin(lowTiles, highTiles).tiles(wound01)

  /** Centroid of the threats, single or many: one answer, no convergence split. */
  def focus(threats: Seq[Pixel]): Option[Pixel] =
    if (threats.isEmpty) None else Some(Maff.centroid(threats))

  /** Raw flee point, past the margin on the own-focus line. */
  def fleeFrom(own: Pixel, focus: Pixel, marginTiles: Double): Pixel =
    own.project(focus, -marginTiles * 32)

  /**
   * Flee destination, if any threat is inside the trigger: centroid,
   * wound-scaled margin (custom bounds for eyes that never shoot back),
   * corner-guarded on the caller's trap width. Nothing in range means nothing.
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
    triggerTiles: Double = AirFlee.triggerTiles,
    cornerTiles: Double = CornerAntiStuckGuard.defaultTiles): Option[Pixel] = {
    val near = threats.filter(t => own.pixelDistance(t) <= triggerTiles * 32.0)
    focus(near).map(focus =>
      CornerAntiStuckGuard.guard(fleeFrom(own, focus, marginTiles(wound01, marginLowTiles, marginHighTiles)), mapWpx, mapHpx, cornerTiles))
  }

  /**
   * Scored flee, one mechanism for every air doctrine: the caller scores
   * each threat (penetration past its trigger, or plain distance) and owns
   * the latch, AirFlee runs from the worst one with corner-guarded legs.
   * The Shuttle passes penetration scores through its exit latch; a plain
   * distance score with a sharp edge keeps the old unlatched behavior.
   * `snap` adapts the projected leg (the Shuttle snaps it walkable);
   * suites pass identity and pin exact pixels.
   */
  def fleeScored(
    own: Pixel,
    scored: Seq[(Pixel, Double)],
    fires: Double => Boolean,
    marginPixels: Double,
    mapWpx: Int,
    mapHpx: Int,
    cornerTiles: Double = CornerAntiStuckGuard.defaultTiles,
    snap: Pixel => Pixel = identity): Option[Pixel] = {
    val worst = scored.view.map(_._2).reduceOption(_ max _).getOrElse(Double.NegativeInfinity)
    if ( ! fires(worst)) None
    else scored.reduceOption((a, b) => if (a._2 >= b._2) a else b)
      .map(focus => CornerAntiStuckGuard.guard(snap(own.project(focus._1, -marginPixels)), mapWpx, mapHpx, cornerTiles))
  }
}
