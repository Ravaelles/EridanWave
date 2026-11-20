package Tactic.Squads.Decisions

import Utilities.Latch

/**
 * Armed cordon (no game access): a lone hull never marches at the goal
 * with a gun inside twenty tiles that could shoot it - the march owns
 * out-of-contact loners, but contact starts at gun reach, not at the
 * duel. Only visible guns count: a fogged memory pins nothing (ghosts
 * own the recall horizon, not this gate), and only guns that could hit
 * this hull count (eyes and harmless statics never dress the line).
 * Latched like the lone-march twin: firing past 21, releasing inside
 * 19, holding the side in between, so a gun breathing on the boundary
 * cannot flap the march every frame.
 */
object ArmedCordon {

  /** The cordon itself, in tiles: no visible gun this close or closer. */
  val cordonTiles: Double = 20.0

  /** March past guns past this distance, in tiles. */
  val clearTiles: Double = 21.0

  /** Hold the march with guns inside this distance, in tiles. */
  val threatTiles: Double = 19.0

  def shouldClearArmed(nearestTiles: Double): Boolean =
    nearestTiles > cordonTiles

  private val clear = new Latch

  def reset(): Unit = clear.reset()

  def clearOfArmed(unitId: Int, nearestTiles: Double): Boolean =
    clear(
      unitId,
      fires    = nearestTiles > clearTiles,
      releases = nearestTiles < threatTiles,
      initial  = shouldClearArmed(nearestTiles))
}
