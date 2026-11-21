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

  /** At least this many warriors to post a line instead of camping. */
  val minWarriors: Int = 4

  /** At most this many warriors to hold the choke instead of forming up. */
  val maxWarriors: Int = 8

  /** At most this army score to hold the choke instead of forming up. */
  val maxUnitsScore: Double = 160.0

  /**
   * Choke hold (pure numbers for the suite): Zerg enemy, four to eight
   * warriors, a small score - post the line on the main choke. Below
   * four there is no line, only a donation; past eight the army forms
   * up instead.
   */
  def shouldHold(enemyIsZerg: Boolean, warriors: Int, unitsScore: Double): Boolean =
    enemyIsZerg && warriors >= minWarriors && warriors <= maxWarriors && unitsScore <= maxUnitsScore

  /** A holder below this total health walks home instead of posting. */
  val minHolderHp: Int = 30

  /** A holder past this, in tiles, from the choke point never posts. */
  val postRadiusTiles: Double = 1.0

  /** A holder needs a friendly combat unit inside this, in tiles. */
  val packRadiusTiles: Double = 0.5

  /**
   * Holder gate (pure numbers for the suite): only a healthy hull
   * posted at the choke with a mate in shoulder reach holds it. A
   * dying, stray or lone hull never pins the line - it fights or
   * withdraws on the normal rules instead of donating under commit.
   */
  def shouldHoldUnit(totalHp: Int, distToChokeTiles: Double, nearestMateTiles: Double): Boolean =
    totalHp >= minHolderHp && distToChokeTiles <= postRadiusTiles && nearestMateTiles <= packRadiusTiles
}
