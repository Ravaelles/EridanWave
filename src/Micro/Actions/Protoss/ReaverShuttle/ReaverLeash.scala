package Micro.Actions.Protoss.ReaverShuttle

/**
 * Reaver leash (no game access): a hull on foot belongs to the ball -
 * past three tiles from its squad centroid it walks back instead of
 * milling alone. Boarding, fighting and holding keep their explicit
 * orders (the leash only owns the unclaimed drift); a raiding pair is
 * its own squad, so its centroid travels with it and the leash stays
 * quiet.
 */
object ReaverLeash {

  /** A foot hull past this far from its squad centroid rejoins, in tiles. */
  val leashTiles: Double = 3.0

  def shouldRejoin(distTiles: Double): Boolean =
    distTiles > leashTiles
}
