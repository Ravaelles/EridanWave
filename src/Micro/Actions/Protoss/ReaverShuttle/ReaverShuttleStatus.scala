package Micro.Actions.Protoss.ReaverShuttle

/**
 * Shared Shuttle+Reaver pair status (no game access): one answer for both
 * hulls - raid the enemy base or support the main army. The oldest Reaver
 * raids; every other pair supports. Either hull below half life defaults
 * to the army: a bleeding pair donates itself abroad, next to the army
 * it still shoots. New land/attack/reboard rules read this instead of
 * deciding alone per frame.
 */
object ReaverShuttleStatus {

  sealed trait PairStatus

  case object RaidBase extends PairStatus

  case object SupportArmy extends PairStatus

  /** Either hull below this health fraction sends the pair to the army. */
  val retreatHealthFraction: Double = 0.5

  def decide(isOldestReaver: Boolean, shuttleHealth01: Double, reaverHealth01: Double): PairStatus =
    if (shuttleHealth01 < retreatHealthFraction || reaverHealth01 < retreatHealthFraction) SupportArmy
    else if (isOldestReaver) RaidBase
    else SupportArmy
}
