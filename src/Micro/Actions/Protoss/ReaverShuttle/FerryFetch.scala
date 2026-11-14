package Micro.Actions.Protoss.ReaverShuttle

/**
 * Stranded-Reaver fetch (no game access): a Shuttle parked by the main
 * ramp with no pair cargo is a wasted hull, and a Reaver that cannot pass
 * the choke walks at it forever instead of boarding. When the ride is
 * standing still at home and the ground is clear, it goes and picks the
 * Reaver up - a ferry that never hops on a fight, only on an idle ride.
 *
 * The caller feeds straight-line tiles to our main, whether the hull is
 * stationary, the Reaver's distance and the nearest ground enemy.
 */
object FerryFetch {

  /** The ride counts as parked at home inside this many tiles of our main. */
  val mainTiles: Double = 30.0

  /** A Reaver this close is the one the hull goes for, in tiles. */
  val reaverTiles: Double = 20.0

  /** No ground enemy closer than this, in tiles: the pickup must be free. */
  val clearTiles: Double = 11.0

  /** Velocity below this is standing still, in pixels per frame. */
  val stationarySpeed: Double = 0.5

  def shouldFetch(
    shuttleToMainTiles : Double,
    stationary         : Boolean,
    reaverDistTiles    : Double,
    groundFoeTiles     : Double): Boolean =
    stationary &&
      shuttleToMainTiles <= mainTiles &&
      reaverDistTiles <= reaverTiles &&
      groundFoeTiles >= clearTiles
}
