package Gameplans.Protoss.PvP

/**
 * Dragoons before Reavers (no game access): the first Reaver waits for
 * a dragoon backbone - fifteen hulls, or a scouted Cannon to break -
 * because a Bay the army cannot cover and a hull with no wall to break
 * trade gas for nothing. Committed reaver play (hulls already out)
 * keeps its own say; this only gates the first hull.
 */
object DragoonsBeforeReavers {

  /** The first Reaver needs this many Dragoons without a scouted Cannon. */
  val minDragoons: Int = 15

  /**
   * Allow the first Reaver (pure numbers for the suite): fifteen
   * Dragoons, or a known enemy Cannon the Reaver answers.
   */
  def allowReavers(dragoons: Int, enemyCannonKnown: Boolean): Boolean =
    enemyCannonKnown || dragoons >= minDragoons
}
