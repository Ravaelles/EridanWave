package Micro.Targeting

/**
 * Pure Dark Templar tank doctrine (no game access): a DT always hits what
 * it can reach (1.1 tiles), then walks to sieged tanks inside 6 tiles,
 * then to unsieged ones. Cloaked anti-tank picks the targets that trade
 * best instead of dueling whatever stands nearest. Distances in tiles.
 */
object DarkTemplarTanks {

  /** Whatever sits this close can be hit right now and goes first. */
  val hittableTiles: Double = 1.1

  /** Sieged tanks inside this radius outrank every other march target. */
  val siegedTankTiles: Double = 6.0

  /** Unsieged tanks inside this radius go after the sieged ones. */
  val unsiegedTankTiles: Double = 6.0

  def hittable(distTiles: Double): Boolean = distTiles <= hittableTiles

  def siegedNear(distTiles: Double): Boolean = distTiles <= siegedTankTiles

  def unsiegedNear(distTiles: Double): Boolean = distTiles <= unsiegedTankTiles
}
