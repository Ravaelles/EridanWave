package Micro.Actions.Protoss.ReaverShuttle

/**
 * Hitchhiker-ditch discipline (pure tiles for the suite): a shuttle
 * never dumps cargo under guns. Aboard a flying hull beats standing
 * under them - the dump waits for clear ground instead of donating
 * the rescue on the pad.
 */
object ShuttleDitch {

  /** Dump only past this much clear ground, in tiles. */
  val clearTiles: Double = 10.0

  def clearToLand(nearestGroundThreatTiles: Double): Boolean =
    nearestGroundThreatTiles > clearTiles
}
