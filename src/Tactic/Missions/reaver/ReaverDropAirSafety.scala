package Tactic.Missions.reaver

/**
 * Pure rule for the independent Shuttle-Reaver cluster (no game access).
 *
 * A lone Shuttle-Reaver cluster operates on its own until enemy air shows
 * up on the map; then it must join the main army instead of feeding harass:
 * any Scourge, 4+ Mutalisks, 2+ Wraiths, 3+ Corsairs. The caller counts
 * visible enemies; this only applies the thresholds. That split keeps the
 * whole rule unit-testable.
 */
object ReaverDropAirSafety {

  /**
   * @param scourge   visible Zerg Scourge
   * @param mutalisks visible Zerg Mutalisks
   * @param wraiths   visible Terran Wraiths
   * @param corsairs  visible Protoss Corsairs
   * @return true when the cluster must stand down into the main army
   */
  def mustJoinArmy(scourge: Int, mutalisks: Int, wraiths: Int, corsairs: Int): Boolean =
    scourge > 0 || mutalisks >= 4 || wraiths >= 2 || corsairs >= 3
}
