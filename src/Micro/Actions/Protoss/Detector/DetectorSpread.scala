package Micro.Actions.Protoss.Detector

import Mathematics.Maff
import Mathematics.Points.Pixel

/**
 * Pure detector spread (no game access): Atlantis SpreadOutDetectors keeps
 * same-type eyes nine tiles apart, and only the newer unit moves - the
 * older holds its watch. Tiles in, tiles out; pixels only convert to call
 * the engine project (rule 10).
 */
object DetectorSpread {

  /** Same-type eyes keep at least this apart, in tiles. */
  val minDistTiles: Double = 9.0

  def shouldSpread(newerId: Int, olderId: Int, distTiles: Double): Boolean =
    newerId > olderId && distTiles < minDistTiles

  /** An older eye we may need to spread from. */
  final case class OlderEye(id: Int, pixel: Pixel, distTiles: Double)

  /** The nearest older eye, if any: only older neighbors count, the newer moves. */
  def partner(usId: Int, others: Seq[OlderEye]): Option[OlderEye] =
    Maff.minBy(others.filter(_.id < usId))(_.distTiles)

  /** Full separation along the ray from the older eye through us. */
  def awayFrom(us: Pixel, them: Pixel): Pixel =
    them.project(us, minDistTiles * 32.0)
}
