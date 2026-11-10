package Micro.Actions.Protoss.Reaver

/**
 * Pure healthy-drop decision (no game access): a healthy Shuttle plus a
 * healthy Reaver must not circle forever taking potshots from a lone
 * Hydralisk. Guns decide, in two rings: the inner ring counts everything
 * (melee chews the landed Reaver), the outer ring counts ranged threats
 * only (melee at six-plus tiles cannot touch the Shuttle, and the Reaver
 * outranges it at eight). The firepower check prices the shot over one
 * drop cycle; the staleness rule stops the dithering when a healthy
 * Reaver sat unfired. Enemy ground statics veto everything.
 */
object ReaverDropBravery {

  /** Inner ring, in tiles: at most one foe of any kind inside. */
  val closeDist: Double = 6.0
  val maxFoesClose: Int = 1

  /** Outer ring, in tiles: at most two ranged threats inside. */
  val rangedDist: Double = 8.0
  val maxRangedNear: Int = 2

  /** No drop this close, in tiles, to enemy ground statics. Overrides all. */
  val staticDefenseDist: Double = 8.0

  /** Both hulls need at least this many shields to risk the drop. */
  val minShields: Int = 50

  /**
   * Tank theater, in tiles: only Siege Tanks this close to the Shuttle
   * vote. A battery across the map cannot hit the drop; stale intel that
   * lingers on the registry cannot veto it either (seam filters).
   */
  val tankTheaterTiles: Double = 20.0

  /**
   * Vs Terran the Scarab must beat Siege Tanks: more than a pair on the
   * field vetoes the brave drop. Other matchups ignore the tank count.
   */
  val maxTerranTanks: Int = 2

  /**
   * A Reaver unfired for more than this long that still holds most of its
   * health gets the drop even when the rings refuse it: sitting aboard
   * forever helps nobody. Frames and wound fraction over hit points plus
   * shields; never-fired hulls do not count (seam passes hasFired).
   */
  val staleFrames: Int = 10 * 24
  val maxStaleWound: Double = 0.5

  /**
   * Frames of exposure for one drop cycle: unload, Scarab flight,
   * reboard. The pair must stomach that long under the nearby guns.
   */
  val exposureFrames: Int = 3 * 24

  /** Keep this multiple of the expected damage as a safety margin. */
  val safetyMargin: Double = 2.0

  /**
   * Ranged-threat classification on primitives, from the Shuttle's seat:
   * only guns that bear on it count, plus the Scourge, which carries no
   * weapon in the data yet kills Shuttles. Ground-only guns (tanks,
   * sunkens) and unarmed spotters do not threaten the hull in the air;
   * the landed Reaver below is covered by the close ring instead.
   */
  def isRangedThreat(canAttack: Boolean, melee: Boolean, flying: Boolean, canHitShuttle: Boolean, isScourge: Boolean): Boolean =
    (canAttack && canHitShuttle && ( ! melee || flying)) || (flying && isScourge)

  def shouldDrop(
    foesClose        : Int,
    rangedNear       : Int,
    shuttleShields   : Int,
    reaverShields    : Int,
    isTerran         : Boolean = false,
    enemyTanks       : Int = 0,
    nearStaticDefense: Boolean = false): Boolean =
    ! nearStaticDefense &&
      foesClose <= maxFoesClose && rangedNear <= maxRangedNear &&
      shuttleShields >= minShields && reaverShields >= minShields &&
      ( ! isTerran || enemyTanks <= maxTerranTanks)

  def mayDropStale(framesSinceFired: Int, woundFraction: Double, hasFired: Boolean = true): Boolean =
    hasFired && framesSinceFired > staleFrames && woundFraction < maxStaleWound

  /**
   * @param ourHitPoints shields plus hull of the Shuttle taking the fire
   * @param enemyDps     summed damage per frame aimed at us while exposed
   * @return true when the expected damage fits inside the margin
   */
  def canAffordShot(ourHitPoints: Double, enemyDps: Double, exposure: Int = exposureFrames, margin: Double = safetyMargin): Boolean =
    ourHitPoints > enemyDps * exposure * margin
}
