package Tactic.Squads.Decisions

/**
 * Middle-choke guard (no game access): past a dozen warriors the standing
 * guard belongs on the middle choke, not the natural - meeting the enemy
 * two chokes out keeps fights off the probes and the wall, and a mid-size
 * army that camps its own ramp concedes the whole map for nothing. Below
 * the line the guard keeps the natural (or the main-choke hold takes
 * the handful); above it the line steps out.
 */
object MiddleChokeHold {

  /** Above this many warriors the guard posts mid-map, in warriors. */
  val guardWarriors: Int = 13

  /**
   * Guard post (pure numbers for the suite): more than a baker's dozen
   * of warriors holds the middle choke instead of the natural.
   */
  def shouldHold(warriors: Int): Boolean =
    warriors > guardWarriors
}
