package Tactic.Squads.Decisions

/**
 * Pure main-march gate (no game access): past 90 supply a unit sitting
 * within 35 ground tiles of our main never dresses, rallies or walks to
 * a formation slot - the pack is big enough to travel as a column, and
 * slot-dancing inside the main only blocks the ramp and the mining line.
 * The caller measures supply and ground distance; this only combines them.
 */
object MainMarch {

  /** Past this supply the army marches as a column, not a formation. */
  val columnSupply: Int = 90

  /** Inside this ground distance to our main, in tiles, slots are skipped. */
  val homeRadiusTiles: Double = 35.0

  def shouldSkip(supplyUsed: Int, distToMainTiles: Double): Boolean =
    supplyUsed > columnSupply && distToMainTiles <= homeRadiusTiles
}
