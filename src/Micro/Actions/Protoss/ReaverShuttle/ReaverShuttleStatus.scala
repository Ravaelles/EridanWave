package Micro.Actions.Protoss.ReaverShuttle

/**
 * Shared Shuttle+Reaver pair status (no game access): one answer for both
 * hulls - raid the enemy base or support the main army. The oldest Reaver
 * raids; every other pair supports. Either hull below half life sends the
 * pair to the army; a raiding pair only goes back out past the return
 * line. The deadband between the two lines is the whole point: shields
 * regenerate and combat damage jumps, so a single line flips the pair's
 * orders on every wound crossing it (the PairRide/RaidFerry flap) - the
 * band holds the current assignment instead. New land/attack/reboard
 * rules read this instead of deciding alone per frame. The matchup rule
 * (never raid into Zerg) is a pair doctrine read, owned by
 * `ReaverShuttlePair`, not by this mapper.
 */
object ReaverShuttleStatus {

  sealed trait PairStatus

  case object RaidBase extends PairStatus

  case object SupportArmy extends PairStatus

  /** Either hull below this health fraction sends the pair to the army. */
  val retreatHealthFraction: Double = 0.5

  /** Both hulls above this health fraction send the oldest pair back out. */
  val returnHealthFraction: Double = 0.65

  /** Start raiding: oldest pair, both hulls healed past the return line. */
  def shouldRaid(isOldestReaver: Boolean, shuttleHealth01: Double, reaverHealth01: Double): Boolean =
    isOldestReaver && shuttleHealth01 >= returnHealthFraction && reaverHealth01 >= returnHealthFraction

  /** Stop raiding: lost the oldest seat, or either hull bleeding. */
  def shouldSupport(isOldestReaver: Boolean, shuttleHealth01: Double, reaverHealth01: Double): Boolean =
    ! isOldestReaver || shuttleHealth01 < retreatHealthFraction || reaverHealth01 < retreatHealthFraction
}
