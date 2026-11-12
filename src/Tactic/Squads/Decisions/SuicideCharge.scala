package Tactic.Squads.Decisions

/**
 * Pure suicide-charge gate (no game access): ground warriors this close,
 * in land tiles, to the Zerg main never retreat, hold formation or dress
 * the line - they only attack. Distances in tiles; the caller measures
 * along the ground, never as the flies.
 */
object SuicideCharge {

  /** At or below this land distance to the Zerg main the charge is on. */
  val chargeTiles: Double = 30.0

  def charging(distTiles: Double): Boolean =
    distTiles <= chargeTiles
}
