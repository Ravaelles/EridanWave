package Micro.Actions.Protoss.Reaver

/**
 * Pure healthy-drop decision (no game access): a healthy Shuttle plus a
 * healthy Reaver must not circle forever taking potshots from a lone
 * Hydralisk. At most two nearby enemies and shields up on both means the
 * drop goes in; the firepower check below prices the shot we are about
 * to take so a healthy pair still refuses a real firing squad.
 */
object ReaverDropBravery {

  /** Enemies this close, in tiles, to the Shuttle count against the drop. */
  val radiusTiles: Double = 12.0

  /** More than this many nearby enemies keeps the Shuttle flying. */
  val maxEnemiesNear: Int = 2

  /** Both hulls need at least this many shields to risk the drop. */
  val minShields: Int = 50

  /**
   * Vs Terran the Scarab must beat Siege Tanks: more than a pair on the
   * field vetoes the brave drop. Other matchups ignore the tank count.
   */
  val maxTerranTanks: Int = 2

  /**
   * Frames of exposure for one drop cycle: unload, Scarab flight,
   * reboard. The pair must stomach that long under the nearby guns.
   */
  val exposureFrames: Int = 3 * 24

  /** Keep this multiple of the expected damage as a safety margin. */
  val safetyMargin: Double = 2.0

  def shouldDrop(
    enemyCount    : Int,
    shuttleShields: Int,
    reaverShields : Int,
    isTerran      : Boolean = false,
    enemyTanks    : Int = 0): Boolean =
    enemyCount <= maxEnemiesNear && shuttleShields >= minShields && reaverShields >= minShields &&
      ( ! isTerran || enemyTanks <= maxTerranTanks)

  /**
   * @param ourHitPoints shields plus hull of the Shuttle taking the fire
   * @param enemyDps     summed damage per frame aimed at us while exposed
   * @return true when the expected damage fits inside the margin
   */
  def canAffordShot(ourHitPoints: Double, enemyDps: Double, exposure: Int = exposureFrames, margin: Double = safetyMargin): Boolean =
    ourHitPoints > enemyDps * exposure * margin
}
