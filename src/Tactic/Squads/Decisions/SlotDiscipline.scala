package Tactic.Squads.Decisions

/**
 * Slot discipline (no game access): the single gate every formation leg
 * shares. Dressing, rallying and returning only yield to an actual shot -
 * a hot gun with a hittable target outranks the pose - to fresh wounds,
 * and to a held retreat verdict that already owns the frame. Mere
 * contact no longer vetoes: a target on the ladder, a cooling weapon or
 * a distant threat never stopped PurpleWave from dressing, and the line
 * only exists if it forms while the enemy is still out there.
 */
object SlotDiscipline {

  /** Damage older than this stops vetoing formation legs. */
  val damageSilenceFrames: Int = 60

  /**
   * Dressing stops inside this distance to an enemy building, in tiles:
   * at the wall the breach outvalues the line - every step to a slot
   * is a shot the depot never takes.
   */
  val buildingVetoTiles: Double = 8.0

  /**
   * Melee never dresses inside this distance to an enemy, in tiles:
   * a blade on guns or masonry trades the kill for a walk home.
   */
  val meleeEnemyVetoTiles: Double = 4.0

  def nearBuilding(distTiles: Double): Boolean =
    distTiles <= buildingVetoTiles

  def meleeEngaged(isMelee: Boolean, distTiles: Double): Boolean =
    isMelee && distTiles <= meleeEnemyVetoTiles

  def maySlot(
    firingNow: Boolean,
    framesSinceDamage: Int,
    verdictHeld: Boolean,
    nearEnemyBuildingTiles: Double = Double.PositiveInfinity,
    isMelee: Boolean = false,
    nearEnemyTiles: Double = Double.PositiveInfinity): Boolean =
    ! firingNow &&
      framesSinceDamage >= damageSilenceFrames && ! verdictHeld &&
      ! nearBuilding(nearEnemyBuildingTiles) &&
      ! meleeEngaged(isMelee, nearEnemyTiles)
}
