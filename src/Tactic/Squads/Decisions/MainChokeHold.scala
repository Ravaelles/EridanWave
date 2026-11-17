package Tactic.Squads.Decisions

/**
 * Main-choke hold (no game access): against Zerg, while the army is
 * still a handful, fighters belong on the main choke - not in the base
 * (lings run past a base camp and chew probes) and not out on the map
 * (a lone pack gets surrounded). The choke funnels lings into zealot
 * blades; five hulls hold what twenty chase. Fires before any scout
 * confirmation is needed beyond the race: a proxy-gate Zerg and a
 * 4-pool both arrive before scouting pays, and holding the choke is
 * never wrong against Zerg while weak. Releases the moment the army
 * outgrows the handful or the score says we stand elsewhere.
 */
object MainChokeHold {

  /** At most this many warriors to hold the choke instead of forming up. */
  val maxWarriors: Int = 5

  /** At most this army score to hold the choke instead of forming up. */
  val maxUnitsScore: Double = 160.0

  /**
   * Choke hold (pure numbers for the suite): Zerg enemy, a handful of
   * warriors, a small score - post the line on the main choke.
   */
  def shouldHold(enemyIsZerg: Boolean, warriors: Int, unitsScore: Double): Boolean =
    enemyIsZerg && warriors <= maxWarriors && unitsScore <= maxUnitsScore
}
