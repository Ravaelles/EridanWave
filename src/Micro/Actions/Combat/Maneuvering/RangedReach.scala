package Micro.Actions.Combat.Maneuvering

/**
 * Shared read of "this weapon reaches": anything beyond one tile counts as
 * ranged for evade and kiting rules. One definition so the Dark Templar,
 * Carrier and Shuttle rules cannot drift apart on where melee ends.
 */
object RangedReach {

  /** Weapon reach above this, in pixels, counts as ranged. */
  val reachPixels: Double = 32.0

  def isRanged(rangePixels: Double): Boolean = rangePixels > reachPixels
}
