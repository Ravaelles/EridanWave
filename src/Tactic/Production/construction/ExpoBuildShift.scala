package Tactic.Production.construction

/**
 * Pure expo-deadlock numbers (no game access): an invisible burrowed
 * blocker passes every placement filter, so the builder camps a dead tile
 * forever. Past this many arrived frames with no trainee, a town hall
 * shifts to the nearest buildable footprint inside four tiles instead of
 * waiting for eyes that may never come.
 */
object ExpoBuildShift {

  /** Builder on site this long with nothing rising means blocked. */
  val stallFramesNeeded: Int = 72

  /** Builder inside this range, in tiles, counts as arrived. */
  val arriveDistTiles: Double = 6.0

  /** Shifted base stays inside this radius, in tiles, of the base spot. */
  val maxShiftTiles: Int = 4

  def stalled(arrivedFrames: Int): Boolean =
    arrivedFrames > stallFramesNeeded
}
