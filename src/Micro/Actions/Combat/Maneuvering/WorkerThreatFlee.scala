package Micro.Actions.Combat.Maneuvering

/**
 * Pure worker threat-flee thresholds (no game access): a Mutalisk pack
 * cannot be fought or outrun, a sieged tank shelling the line outranges
 * any response, and a storm on the worker needs no deliberation.
 * Distances in tiles.
 */
object WorkerThreatFlee {

  /** This many Mutalisks near one miner means the pack. */
  val mutaPackSize: Int = 3

  /** Mutalisks inside this radius, in tiles, count for the pack. */
  val mutaPackTiles: Double = 10.0

  /** Shelling counts inside tank range plus this margin, in tiles. */
  val tankMarginTiles: Double = 2.0

  def mutaPack(mutasNear: Int): Boolean =
    mutasNear >= mutaPackSize

  def tankShelling(distTiles: Double, tankRangeTiles: Double): Boolean =
    distTiles < tankRangeTiles + tankMarginTiles
}
