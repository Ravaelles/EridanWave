package Micro.Actions.Protoss.ReaverShuttle

/**
 * Ferry gate (no game access): a Reaver rides only when it has ground to
 * cover. Short hops walk - boarding, flying and landing for two tiles
 * trades frames for nothing while the Shuttle idles overhead.
 */
object ReaverShuttleFerry {

  /** Ride only past this travel distance, in tiles. */
  val rideMinTiles: Double = 3.0

  def needsRide(distTiles: Double): Boolean =
    distTiles > rideMinTiles
}
