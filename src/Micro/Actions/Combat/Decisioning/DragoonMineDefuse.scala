package Micro.Actions.Combat.Decisioning

/**
 * Mine defuse (no game access): a Dragoon that spots a detected Spider
 * Mine inside ten tiles shoots it instead of walking into it - but only
 * with no enemy that can answer inside twelve. Defusing under guns
 * trades the hull for the mine; the duel owns that frame instead.
 * Distances in tiles.
 */
object DragoonMineDefuse {

  /** Shoot detected mines this close, in tiles. */
  val defuseTiles: Double = 10.0

  /** Only with no answering enemy inside this, in tiles. */
  val safeTiles: Double = 12.0

  def inDefuseRange(distTiles: Double): Boolean =
    distTiles <= defuseTiles

  def safeToDefuse(nearestThreatTiles: Double): Boolean =
    nearestThreatTiles > safeTiles
}
