package Micro.Actions.Transportation

/**
 * Pure drop-zone veto (no game access): a flying transport never unloads
 * into the reach of enemy anti-ground combat buildings - the Shuttle and
 * its cargo die for nothing. The caller applies it to flying transports
 * only: a grounded unload at home is unaffected. The berth is edge to edge,
 * in tiles.
 */
object DropSafety {

  /** No unloading this close to an enemy anti-ground combat building, in tiles. */
  val staticBerthTiles: Double = 8.1

  def vetoLanding(nearestEnemyStaticTiles: Option[Double]): Boolean =
    nearestEnemyStaticTiles.exists(_ < staticBerthTiles)
}
