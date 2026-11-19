package Micro.Actions.Protoss.ReaverShuttle

/**
 * Pair drop spot (no game access): a loaded Shuttle never flies at the
 * static itself - the old duel ferry aimed at the nearest known enemy
 * pixel, parked over the Sunken and circled until something died. The
 * ring below designates the landing instead: eight tiles around the
 * nearest static, never closer than the floor, clear of mobile guns,
 * nearest our side of the map. The ring sits past the DropSafety berth
 * so the unload order fires on arrival, and inside the engage band so
 * the hull walks the last tiles instead of reboarding. The caller feeds
 * distances; this only generates the ring and picks the spot.
 */
object ReaverDropSpot {

  /** Never land closer to a static than this, in tiles. */
  val staticFloorTiles: Double = 7.5

  /** Keep this clear of enemy mobile warriors, in tiles. */
  val mobileMarginTiles: Double = 4.0

  /**
   * Drop ring around the static, in tiles: past the 8.1 DropSafety
   * berth so the unload is legal on arrival, inside the 12-tile
   * engage band so the pair commits instead of reboarding.
   */
  val ringTiles: Double = 8.5

  /** Spots around the ring. */
  val ringSpots: Int = 8

  final case class Candidate(
    xTiles: Double,
    yTiles: Double,
    distStaticTiles: Double,
    distMobileTiles: Double,
    distHomeTiles: Double)

  /** Ring points around the static, in tiles. */
  def ring(staticTile: (Double, Double)): Seq[(Double, Double)] =
    (0 until ringSpots).map(i => {
      val angle = 2.0 * Math.PI * i / ringSpots
      (staticTile._1 + ringTiles * Math.cos(angle),
        staticTile._2 + ringTiles * Math.sin(angle))
    })

  def isValid(distStaticTiles: Double, distMobileTiles: Double): Boolean =
    distStaticTiles >= staticFloorTiles && distMobileTiles >= mobileMarginTiles

  /** Nearest-home valid spot; nothing when every point breaks a margin. */
  def pick(candidates: Seq[Candidate]): Option[Candidate] = {
    val valid = candidates.filter(c => isValid(c.distStaticTiles, c.distMobileTiles))
    if (valid.isEmpty) None else Some(valid.minBy(_.distHomeTiles))
  }
}
