package Lifecycle

import Utilities.Time.Seconds

/**
 * Hopeless-game concession (no game access): a pure port of Atlantis's
 * `GG.applies` - concede a game that is lost beyond any macro comeback
 * instead of grinding it to the last worker. Every clause has a job:
 * late (or already workerless) so rushes never trigger it, throttled so
 * the check costs one frame in 128, routed (relative strength in single
 * digits, or nearly out of workers) so only a collapse qualifies, and
 * then three sanity gates - a real enemy army, outnumbered eight to one,
 * nothing left to rebuild with. Numbers are Atlantis's, translated
 * one to one; the one deliberate mapping is documented on the strength
 * input (relative percent, not an absolute score).
 */
object HopelessGame {

  /** Late game: no coming back through macro, only through a miracle. */
  val lateFrame: Int = Seconds(400)()

  /** The check costs one frame in this many. */
  val checkEveryFrames: Int = 128

  /** Relative army strength at or below this percent is a rout. */
  val routStrengthPct: Double = 8.0

  /** Nearly out of workers, whatever the army says. */
  val workerFloor: Int = 10

  /** A real enemy army, not a scout party. */
  val enemyArmyFloor: Int = 15

  /** Outnumbered this badly in combat units, the duel is over. */
  val routRatio: Int = 8

  /** Nothing left to rebuild with at or under this many hulls. */
  val remnantCap: Int = 30

  /**
   * Concede (pure flags for the suite). relativeStrengthPct is
   * `ArmyStrength.score` semantics - our power as a percent of theirs -
   * which is exactly what the Atlantis `Army.strength() <= 8` arm reads.
   */
  def shouldConcede(
    frame: Int,
    ourWorkers: Int,
    ourCombatUnits: Int,
    enemyCombatUnits: Int,
    relativeStrengthPct: Double): Boolean =
    (frame >= lateFrame || ourWorkers == 0) &&
      frame % checkEveryFrames == 0 &&
      (relativeStrengthPct <= routStrengthPct || ourWorkers <= workerFloor) &&
      enemyCombatUnits >= enemyArmyFloor &&
      ourCombatUnits * routRatio <= enemyCombatUnits &&
      ourCombatUnits + ourWorkers <= remnantCap
}
