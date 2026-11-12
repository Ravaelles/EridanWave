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

  /** A miner this hurt never trades with a melee pack alone, in hit points. */
  val fleeHp: Int = 37

  /** This many enemy melee fighters near one miner means a pack. */
  val meleePackSize: Int = 2

  /**
   * A weak miner outnumbers its chances: at fleeHp or less against a melee
   * pack with no fighting warrior nearby there is no winning trade, so it
   * runs instead of potshotting. Supported miners still swarm with the army.
   */
  def doomedVsMelee(totalHealth: Int, meleeNearby: Int, supported: Boolean): Boolean =
    totalHealth <= fleeHp && meleeNearby >= meleePackSize && ! supported

  def mutaPack(mutasNear: Int): Boolean =
    mutasNear >= mutaPackSize

  def tankShelling(distTiles: Double, tankRangeTiles: Double): Boolean =
    distTiles < tankRangeTiles + tankMarginTiles
}
