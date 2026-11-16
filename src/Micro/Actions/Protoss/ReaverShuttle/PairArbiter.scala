package Micro.Actions.Protoss.ReaverShuttle

/**
 * Pair arbiter (pure game-free kernel plus a thin game seam): one decider
 * for two hulls. The Shuttle and the Reaver used to decide alone every
 * frame - the ride fetched whoever hailed while the hull waited under
 * fire for a ride doing something else, and the pair paralyzed itself.
 * Now the pair reads one snapshot and the arbiter issues one order per
 * hull; the brains execute, they do not negotiate. Orders are sealed
 * enums, so at any frame each hull has exactly one named state - the
 * `act` label carries it into the probe log unchanged.
 * Distances in tiles.
 */
object PairArbiter {

  /** The Shuttle's state: exactly one per frame, no hovering without a purpose. */
  sealed trait ShuttleOrder { def label: String }

  /** Fly to the on-foot mate and pick it up. */
  case object FetchMate extends ShuttleOrder { val label = "FetchMate" }

  /** Stay over the mate while it fights or boards. */
  case object GuardMate extends ShuttleOrder { val label = "GuardMate" }

  /** Run from what can hit the hull, cargo or none. */
  case object Flee extends ShuttleOrder { val label = "PairFlee" }

  /** Carry the loaded pair at the enemy base. */
  case object RaidCarry extends ShuttleOrder { val label = "RaidCarry" }

  /** Carry the loaded pair with the army. */
  case object FollowArmy extends ShuttleOrder { val label = "FollowArmy" }

  /** Wait at the named post for the next mate, never drift. */
  case object AwaitPost extends ShuttleOrder { val label = "AwaitPost" }

  /** The Reaver's state: never Wait - without a ride or a fight it walks. */
  sealed trait ReaverOrder { def label: String }

  /** Board the ride in reach right now. */
  case object BoardRide extends ReaverOrder { val label = "PairBoard" }

  /** Fight on foot until the ride arrives. */
  case object FightOnFoot extends ReaverOrder { val label = "PairFight" }

  /** A loaded shot with a foe in reach: hold and spend it. */
  case object HoldShoot extends ReaverOrder { val label = "PairShoot" }

  /** Walk to the ride instead of waiting for it. */
  case object WalkToRide extends ReaverOrder { val label = "PairWalkRide" }

  /** No ride anywhere: walk to the army, never wait. */
  case object WalkToArmy extends ReaverOrder { val label = "PairWalkArmy" }

  /** Stay aboard, the Shuttle owns the frame. */
  case object RideAlong extends ReaverOrder { val label = "PairRide" }

  /** A foe this close to the Reaver means a fight, not a walk. */
  val fightTiles: Double = 8.5

  final case class PairDirective(shuttle: ShuttleOrder, reaver: ReaverOrder)

  /**
   * Facts in, one order per hull out. First rule wins; the order below
   * is the contract. A pressured hull outranks every errand; a loaded
   * pair follows its status; an on-foot mate is fetched even mid-fight;
   * and the last rule is the anti-paralysis guarantee: with no mate the
   * Shuttle holds its post and the Reaver walks to the army.
   */
  def decide(
    loaded: Boolean,
    mateOnFoot: Boolean,
    mateDistTiles: Double,
    reaverFoeTiles: Double,
    reaverGunTiles: Double,
    reaverShotReady: Boolean,
    shuttlePressured: Boolean,
    reaverDoomed: Boolean,
    raid: Boolean): PairDirective = {

    if (shuttlePressured && loaded) {
      return PairDirective(Flee, RideAlong)
    }

    if (shuttlePressured) {
      val reaver =
        if (reaverDoomed && ReaverShuttleReboard.rideInReach(mateDistTiles)) BoardRide
        else if (reaverFoeTiles <= fightTiles) FightOnFoot
        else WalkToRide

      return PairDirective(Flee, reaver)
    }

    if (loaded) {
      return PairDirective(if (raid) RaidCarry else FollowArmy, RideAlong)
    }

    if (mateOnFoot) {
      if (ReaverShuttleReboard.rideInReach(mateDistTiles)) {
        return PairDirective(GuardMate, BoardRide)
      }

      if (reaverFoeTiles <= fightTiles) {
        // Fetch the fighting, not only the hailing: the ride comes to
        // the duel while the hull spends its shot or trades. HoldShoot
        // needs the foe inside the gun, not just inside the fight:
        // past gun reach holding is waiting, and waiting is banned.
        val reaver =
          if (reaverShotReady && reaverFoeTiles <= reaverGunTiles) HoldShoot else FightOnFoot

        return PairDirective(FetchMate, reaver)
      }

      return PairDirective(FetchMate, WalkToRide)

    }

    PairDirective(AwaitPost, WalkToArmy)
  }
}
