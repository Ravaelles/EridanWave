package Tactic.Squads.Decisions

/**
 * March dressing: leaders wait for the pack before first contact.
 *
 * FormationApproach dresses the line once the enemy is known, but at game
 * start nobody has seen anything yet, so the whole squad used to sprint at
 * full speed and arrive as a trickle. Instead compare each unit's distance to
 * the march goal against the pack mean: a unit running ahead of the mean
 * holds position until the pack catches up. Self-regulating (the mean moves
 * with the pack, so nobody waits long) and deadlock-free (units within
 * tolerance always march). SquadRally gathers radial stragglers; this holds
 * axial leaders. Pure decision so it stays unit-testable without a game.
 */
object MarchDress {

  /** Leaders this far ahead of the pack mean (tiles) wait instead of running on. */
  val waitAheadTiles: Double = 2.0

  def shouldWait(ownDistToGoalTiles: Double, meanDistToGoalTiles: Double): Boolean = {
    meanDistToGoalTiles - ownDistToGoalTiles > waitAheadTiles
  }

  /** No dressing without contact: enemies this close, in tiles, justify it. */
  val dressEnemyRadiusTiles: Double = 20.0

  def enemyNear(distTiles: Double): Boolean =
    distTiles <= dressEnemyRadiusTiles

  /** A pack this strong marches through instead of dressing, in value. */
  val strongArmyStrength: Double = 150.0

  /** A pack this compact marches through instead of dressing, in tiles. */
  val compactAllyTiles: Double = 3.0

  /**
   * Compact-strong-unopposed skip (pure numbers for the suite): with no
   * armed enemy in sight, a strong pack marching elbow-to-elbow never
   * dresses - waiting only accordion-marches a formation nobody threatens.
   * The caller feeds live visibility, strength and spacing; this only
   * combines them.
   */
  def shouldSkipWait(strength: Double, nearestAllyTiles: Double, armedEnemyVisible: Boolean): Boolean =
    ! armedEnemyVisible && strength >= strongArmyStrength && nearestAllyTiles <= compactAllyTiles
}
