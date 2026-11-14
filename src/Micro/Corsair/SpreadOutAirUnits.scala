package Micro.Corsair

import Mathematics.Maff
import Mathematics.Points.Pixel

/**
 * Corsair separation (no game access): same-type hulls keep five tiles
 * apart, and only the newer unit moves - the older holds its hunt.
 * Tighter than the Observer nine: Corsairs mass for Mutalisk packs, but
 * stacked wings eat the same Scourge and turn as one wide target.
 * Tiles in, tiles out; the caller measures and issues the order.
 */
object SpreadOutAirUnits {

  /** Same-type wings keep at least this apart, in tiles. */
  val minDistTiles: Double = 5.0

  def shouldSpread(newerId: Int, olderId: Int, distTiles: Double): Boolean =
    newerId > olderId && distTiles < minDistTiles

  /** An older wing we may need to spread from. */
  final case class OlderWing(id: Int, pixel: Pixel, distTiles: Double)

  /** The nearest older wing, if any: only older hulls count, the newer moves. */
  def partner(usId: Int, others: Seq[OlderWing]): Option[OlderWing] =
    Maff.minBy(others.filter(_.id < usId))(_.distTiles)

  /** Full separation along the ray from the older wing through us. */
  def awayFrom(us: Pixel, them: Pixel): Pixel =
    them.project(us, minDistTiles * 32)
}
