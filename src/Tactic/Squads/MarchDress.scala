package Tactic.Squads

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
}
