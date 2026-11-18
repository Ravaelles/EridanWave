package Micro.Actions.Protoss.ReaverShuttle

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Skirmish.FightOrFlee
import Micro.Actions.Transportation.DropSafety
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

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

  /**
   * Run from what can hit the hull, but TO the on-foot mate, never away:
   * a glued retreat ends in a pickup and a joint flight, never in an
   * abandoned hull. The executor holds the mate first and only flees
   * outright with no mate lock left.
   */
  case object FleeToMate extends ShuttleOrder { val label = "FleeToMate" }

  /** Carry the loaded pair at the enemy base. */
  case object RaidCarry extends ShuttleOrder { val label = "RaidCarry" }

  /** Carry the loaded pair with the army. */
  case object FollowArmy extends ShuttleOrder { val label = "FollowArmy" }

  /** Wait at the named post for the next mate, never drift. */
  case object AwaitPost extends ShuttleOrder { val label = "AwaitPost" }

  /** No pair cargo anywhere: provoke the enemy instead of hovering. */
  case object Bait extends ShuttleOrder { val label = "BaitRide" }

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

  /** Leave the ride on this frame: the landing band is ready or ordered. */
  case object UnloadHere extends ReaverOrder { val label = "PairUnload" }

  /** A foe this close to the Reaver means a fight, not a walk. */
  val fightTiles: Double = 8.5

  /**
   * Pressure band outer edge, in tiles: a threat counts as pressure
   * while its reach plus this band touches the hull - the far edge of
   * the wound-scaled flee band the executor runs (4 fresh, up to 13
   * dying). Capability alone is not pressure: a gun across the map
   * must not rout the pair.
   */
  val fleeBandTiles: Double = 13.0

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
    raid: Boolean,
    baitRide: Boolean,
    unloadOrdered: Boolean = false,
    unloadSpotReady: Boolean = false,
    boardLocked: Boolean = false,
    threatTiles: Double = Double.PositiveInfinity,
    longTrip: Boolean = false,
    wound01: Double = 0.0): PairDirective = {

    if (shuttlePressured && loaded) {
      return PairDirective(Flee, RideAlong)
    }

    if (shuttlePressured && loaded) {
      return PairDirective(Flee, RideAlong)
    }

    if (shuttlePressured) {
      val reaver =
        if (reaverDoomed && ReaverShuttleReboard.rideInReach(mateDistTiles)) BoardRide
        else if (reaverFoeTiles <= fightTiles) FightOnFoot
        else WalkToRide

      // Glued retreat: with the mate on foot inside the band the ride
      // retreats TO it (pickup on arrival, then both fly) instead of away
      // from it - never abandon the hull to save the hull.
      if (PairGlue.fleeToMate(mateOnFoot, mateDistTiles)) {
        return PairDirective(FleeToMate, reaver)
      }

      return PairDirective(Flee, reaver)
    }

    if (loaded) {
      // Landing is an order like any other: the band ready or the
      // mission ordering it puts the hull out, pressure keeps it in.
      val reaver =
        if (unloadOrdered || unloadSpotReady) UnloadHere else RideAlong

      return PairDirective(if (raid) RaidCarry else FollowArmy, reaver)
    }

    if (mateOnFoot) {
      // Doom boards through everything below, pin included: a dying
      // hull takes the ride even mid-duel. The pressured branch above
      // already rescued it under fire; this is the calm-ride twin.
      if (reaverDoomed && ReaverShuttleReboard.rideInReach(mateDistTiles)) {
        return PairDirective(GuardMate, BoardRide)
      }

      if (boardLocked) {
        return PairDirective(AwaitPost, FightOnFoot)
      }

      // An engaged hull finishes its duel first: boarding mid-duel for
      // a trip strands the pair when the landing is vetoed, and the
      // ride already holds overhead for the reboard after the kill.
      if (reaverFoeTiles <= reaverGunTiles) {
        val reaver =
          if (reaverShotReady) HoldShoot else FightOnFoot

        return PairDirective(FetchMate, reaver)
      }

      // Board for a real trip, a lost position, or a rescue - never for
      // a short hop under guns the hull could walk: the old reboard band
      // and the ferry trip rule, now one gate instead of two claims.
      if (ReaverShuttleReboard.rideInReach(mateDistTiles)
        && (ReaverShuttleReboard.mayReboard(threatTiles, wound01) || reaverDoomed || longTrip)) {
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

    if (baitRide) {
      return PairDirective(Bait, WalkToArmy)
    }

    PairDirective(AwaitPost, WalkToArmy)
  }

  /**
   * Plain-data hull views: everything the bond decision needs, nothing
   * it doesn't. Positions in tiles, squads as opaque keys, foes reduced
   * to the three numbers the kernel reads. No game access past this
   * line: `resolve` below is pure and suite-pinned, the mappers beside
   * it are mechanical field reads.
   */
  final case class FoeView(distTiles: Double, foeGunTiles: Double, reaverGunTiles: Double)

  final case class ThreatView(distTiles: Double, rangeTiles: Double)

  final case class ShuttleView(
    posTiles: (Double, Double),
    squadKey: Option[Int],
    hasReaverAboard: Boolean,
    threats: Seq[ThreatView])

  final case class ReaverView(
    posTiles: (Double, Double),
    squadKey: Option[Int],
    loaded: Boolean,
    bond: Option[ShuttleView],
    scarabs: Int,
    activeScarab: Boolean,
    cooldownLeft: Int,
    wound01: Double,
    unloadOrdered: Boolean,
    nearestStaticTiles: Option[Double],
    attackedRecently: Boolean,
    boardLocked: Boolean,
    threatTiles: Double,
    longTrip: Boolean,
    foes: Seq[FoeView])

  private def distTiles(a: (Double, Double), b: (Double, Double)): Double = {
    val dx = a._1 - b._1
    val dy = a._2 - b._2

    Math.sqrt(dx * dx + dy * dy)
  }

  private def nearestMate(shuttle: ShuttleView, reavers: Seq[ReaverView], baseless: Boolean): Option[ReaverView] =
    Maff.minBy(reavers.view.filter(r =>
      ! r.loaded && ReaverShuttlePair.sameSquadOrUnstaffed(shuttle.squadKey, r.squadKey, baseless)))(r => distTiles(shuttle.posTiles, r.posTiles))

  private def nearestFoe(foes: Seq[FoeView]): Option[FoeView] =
    Maff.minBy(foes)(_.distTiles)

  /**
   * Landing spot, pure: the nearest static inside the kill band, quiet
   * hull, no threat inside the wound margin - and past the transport
   * berth, which vetoes the unload itself. The berth reaches further
   * than the gun, so the window is land-outside plus one step in.
   */
  private def spotReady(view: ReaverView): Boolean =
    view.nearestStaticTiles.exists(s =>
      ReaverShuttleStaticAttack.shouldLand(s, view.attackedRecently, view.threatTiles, view.wound01)
        && ! DropSafety.vetoLanding(Some(s)))

  private def shotReady(scarabs: Int, activeScarab: Boolean, cooldownLeft: Int): Boolean =
    scarabs > 0 && ! activeScarab && cooldownLeft <= 0

  private def doomed(wound01: Double, foes: Seq[FoeView]): Boolean =
    wound01 > 0.5 && foes.exists(f => f.distTiles <= f.foeGunTiles)

  private def pressured(threats: Seq[ThreatView]): Boolean =
    threats.exists(t => t.distTiles <= t.rangeTiles + fleeBandTiles)

  /**
   * Bond resolution, Shuttle side (pure): the mate is the nearest
   * unloaded hull in squad, the subject is the mate (a loaded pair
   * needs no reaver facts - the kernel ignores them). The WalkToArmy
   * regression of the first wired game lives here as a pinned row:
   * an unbonded hull with a free same-squad ride walks to the ride,
   * never to the army.
   */
  def resolveShuttle(shuttle: ShuttleView, reavers: Seq[ReaverView], raid: Boolean, baitRide: Boolean, baseless: Boolean = false): PairDirective = {
    val mate = nearestMate(shuttle, reavers, baseless)
    val foe = mate.flatMap(m => nearestFoe(m.foes))
    decide(
      loaded            = shuttle.hasReaverAboard,
      mateOnFoot        = mate.isDefined,
      mateDistTiles     = mate.map(m => distTiles(shuttle.posTiles, m.posTiles)).getOrElse(Double.PositiveInfinity),
      reaverFoeTiles    = foe.map(_.distTiles).getOrElse(Double.PositiveInfinity),
      reaverGunTiles    = foe.map(_.reaverGunTiles).getOrElse(0.0),
      reaverShotReady   = mate.exists(m => shotReady(m.scarabs, m.activeScarab, m.cooldownLeft)),
      shuttlePressured  = pressured(shuttle.threats),
      reaverDoomed      = mate.exists(m => doomed(m.wound01, m.foes)),
      raid              = raid,
      baitRide          = baitRide,
      boardLocked       = mate.exists(_.boardLocked),
      threatTiles       = mate.flatMap(m => Maff.minBy(m.foes)(_.distTiles)).map(_.distTiles).getOrElse(Double.PositiveInfinity),
      longTrip          = false,
      wound01           = mate.map(_.wound01).getOrElse(0.0))
  }

  /**
   * Bond resolution, Reaver side (pure): the bond is the reservation
   * first, the live ride second, the nearest free same-squad ride
   * third - a hull with none of the three walks to the army instead
   * of holding for a pickup that is not coming.
   */
  def resolveReaver(reaver: ReaverView, shuttles: Seq[ShuttleView], raid: Boolean, baseless: Boolean = false): PairDirective = {
    if (reaver.loaded) {
      val out = decide(
        loaded            = true,
        mateOnFoot        = false,
        mateDistTiles     = Double.PositiveInfinity,
        reaverFoeTiles    = Double.PositiveInfinity,
        reaverGunTiles    = 0.0,
        reaverShotReady   = false,
        shuttlePressured  = reaver.bond.exists(b => pressured(b.threats)),
        reaverDoomed      = false,
        raid              = raid,
        baitRide          = false,
        unloadOrdered     = reaver.unloadOrdered,
        unloadSpotReady   = spotReady(reaver))

      return out
    }

    val ride = reaver.bond.orElse(Maff.minBy(shuttles.view.filter(s =>
      ! s.hasReaverAboard && ReaverShuttlePair.sameSquadOrUnstaffed(reaver.squadKey, s.squadKey, baseless)))(s => distTiles(reaver.posTiles, s.posTiles)))
    val foe = nearestFoe(reaver.foes)
    decide(
      loaded            = false,
      mateOnFoot        = ride.isDefined,
      mateDistTiles     = ride.map(r => distTiles(reaver.posTiles, r.posTiles)).getOrElse(Double.PositiveInfinity),
      reaverFoeTiles    = foe.map(_.distTiles).getOrElse(Double.PositiveInfinity),
      reaverGunTiles    = foe.map(_.reaverGunTiles).getOrElse(0.0),
      reaverShotReady   = shotReady(reaver.scarabs, reaver.activeScarab, reaver.cooldownLeft),
      shuttlePressured  = ride.exists(r => pressured(r.threats)),
      reaverDoomed      = doomed(reaver.wound01, reaver.foes),
      raid              = raid,
      baitRide          = false,
      boardLocked       = reaver.boardLocked,
      threatTiles       = reaver.threatTiles,
      longTrip          = reaver.longTrip,
      wound01           = reaver.wound01)
  }

  /** Game mapper: one Shuttle, plain data out. */
  def shuttleOrder(shuttle: FriendlyUnitInfo): ShuttleOrder = {
    val reavers = With.units.ours.view.filter(u => u.alive && Protoss.Reaver(u)).map(reaverView(_)).toSeq
    resolveShuttle(
      shuttleView(shuttle),
      reavers,
      ReaverShuttlePair.statusOfShuttle(shuttle) == ReaverShuttleStatus.RaidBase,
      ReaverShuttlePair.isBaitRide(shuttle),
      With.geography.ourBases.isEmpty).shuttle
  }

  /** Game mapper: one Reaver, plain data out. */
  def reaverOrder(reaver: FriendlyUnitInfo): ReaverOrder = {
    val bond = reaver.agent.ride.orElse(ReaverShuttlePair.rideOf(reaver)).map(shuttleView)
    val rides = With.units.ours.view.filter(u => u.alive && Protoss.Shuttle(u)).map(shuttleView).toSeq
    resolveReaver(
      reaverView(reaver, bond),
      rides,
      ReaverShuttlePair.statusOfReaver(reaver) == ReaverShuttleStatus.RaidBase,
      With.geography.ourBases.isEmpty).reaver
  }

  /**
   * Landing band (game read, shared by the order and its executor):
   * the order opens the attempt on the band or the mission word, the
   * executor re-checks it before the unload goes out.
   */
  def landingReady(reaver: FriendlyUnitInfo): Boolean = {
    val bond = reaver.agent.ride.orElse(ReaverShuttlePair.rideOf(reaver)).map(shuttleView)

    spotReady(reaverView(reaver, bond))
  }

  private def squadKey(unit: FriendlyUnitInfo): Option[Int] =
    unit.squad.map(System.identityHashCode(_))

  private def tilePos(unit: FriendlyUnitInfo): (Double, Double) =
    (unit.pixel.x * Maff.inv32, unit.pixel.y * Maff.inv32)

  private def shuttleView(shuttle: FriendlyUnitInfo): ShuttleView =
    ShuttleView(
      tilePos(shuttle),
      squadKey(shuttle),
      ReaverShuttlePair.passengerOf(shuttle).isDefined,
      shuttle.matchups.threats.view.filter(t => t.likelyStillThere && t.canAttack(shuttle)).map(t =>
        ThreatView(
          shuttle.pixelDistanceEdge(t) * Maff.inv32,
          t.pixelRangeAgainst(shuttle) * Maff.inv32)).toSeq)

  private def reaverView(reaver: FriendlyUnitInfo, bond: Option[ShuttleView] = None): ReaverView =
    ReaverView(
      tilePos(reaver),
      squadKey(reaver),
      reaver.loaded,
      bond,
      reaver.scarabs,
      reaver.hasActiveScarab,
      reaver.cooldownLeft,
      reaver.woundPercent(),
      reaver.agent.fightReason == FightOrFlee.Reave,
      Maff.minBy(With.units.enemy.view.filter(e =>
        e.unitClass.isCombatBuilding && e.canAttackGround && e.likelyStillThere))(reaver.pixelDistanceEdge(_))
        .map(reaver.tileDistanceEdge(_)),
      With.framesSince(reaver.lastFrameTakingDamage) <= 72,
      ReaverBoardGate.denyBoard(reaver),
      Maff.minBy(reaver.matchups.threats.view.filter(t => t.likelyStillThere && t.canAttack(reaver)))(reaver.pixelDistanceEdge(_))
        .map(reaver.pixelDistanceEdge(_) * Maff.inv32).getOrElse(Double.PositiveInfinity),
      ReaverShuttleFerry.needsRide(reaver.pixelDistanceCenter(reaver.agent.destinationNext()) * Maff.inv32),
      reaver.matchups.targets.map(t =>
        FoeView(
          reaver.pixelDistanceEdge(t) * Maff.inv32,
          t.pixelRangeAgainst(reaver) * Maff.inv32,
          reaver.pixelRangeAgainst(t) * Maff.inv32)).toSeq)
}
