package Micro.Actions.Combat.Maneuvering

/**
 * Pure mineral-clearance rule for fleeing (no game access): large land
 * units wedge between mineral fields and die there, so flee spots stay
 * over a tile away from minerals. Workers read as small and keep mining;
 * fliers do not care. Distances in tiles.
 */
object FleeMineralClearance {

  /** Flee spots keep this clearance from minerals, in tiles. */
  val mineralClearTiles: Double = 1.0

  def applies(isLarge: Boolean, flying: Boolean): Boolean =
    isLarge && ! flying

  def clearOfMinerals(mineralAdjacent: Boolean): Boolean =
    ! mineralAdjacent
}
