package Micro.Actions.Protoss.ReaverShuttle

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Latch

/**
 * The pair as one addressable thing (no game decisions here, just
 * lookups): which Reaver rides which Shuttle, who is the oldest, and
 * the shared status both brains read every frame. One place owns the
 * pairing so the two brains cannot disagree about it.
 */
object ReaverShuttlePair {

  /** The combat passenger this Shuttle carries, if any. */
  def passengerOf(shuttle: FriendlyUnitInfo): Option[FriendlyUnitInfo] =
    shuttle.loadedUnits.find(Protoss.Reaver(_))

  /** The transport this Reaver rides, if any. */
  def rideOf(reaver: FriendlyUnitInfo): Option[FriendlyUnitInfo] =
    reaver.transport.flatMap(_.friendly).filter(_.isTransport)
      .orElse(reaver.agent.ride.filter(_.isTransport))

  /** Oldest = lowest id among our live Reavers: it raids, the rest follow. */
  def isOldest(reaver: FriendlyUnitInfo): Boolean =
    With.units.ours.view.filter(u => u.alive && Protoss.Reaver(u)).forall(_.id >= reaver.id)

  // Per-frame scans the pair brain asks for more than once (BeShuttle reads
  // bait and mate every frame per Shuttle): one pass per frame, not per read.
  private val unloadedReavers = new Performance.Cache(() =>
    With.units.ours.view.filter(u => u.alive && Protoss.Reaver(u) && ! u.loaded).toVector)

  private val anyPairCargo = new Performance.Cache(() =>
    With.units.ours.exists(u => Protoss.Reaver(u) || Protoss.HighTemplar(u)))

  /**
   * The on-foot mate: nearest live unloaded Reaver of our squad. After
   * an unload the loaded-passenger bond evaporates, but the pair does
   * not - the ride the Reaver brain assumes (rescue and reboard inside
   * 64 pixels) has to stay its ride until it boards again.
   */
  def mateOnFoot(shuttle: FriendlyUnitInfo): Option[FriendlyUnitInfo] =
    Maff.minBy(unloadedReavers().filter(u => shuttle.squad.exists(u.squad.contains)))(
      _.pixelDistanceCenter(shuttle.pixel))

  /** Stay with an unloaded mate operating this close, in tiles: the ride the Reaver brain assumes. */
  val mateEscortTiles: Double = 15.0

  def shouldEscort(distTiles: Double): Boolean =
    distTiles <= mateEscortTiles

  /**
   * Escort release range, in tiles: once committed the ride holds station
   * until the mate walks out past escort range plus this margin, so the
   * escort/await choice cannot flap every other frame on the boundary.
   */
  val escortReleaseTiles: Double = mateEscortTiles + 2.0

  private val escorting = new Latch

  def reset(): Unit = { escorting.reset() }

  /**
   * Latched escort hold: fires sharp inside escort range, releases past
   * the release margin. A mate blinking across the edge no longer flips
   * the ride between escorting and awaiting every other frame.
   */
  def escortLatched(shuttleId: Int, distTiles: Double): Boolean =
    escorting(
      shuttleId,
      fires   = shouldEscort(distTiles),
      releases = distTiles > escortReleaseTiles)

  /**
   * An idle pair on a baseless map: the Reaver is on foot with nothing
   * to fight and the ride is safe. One side waiting for the other is
   * how duels stall to timeout, so idleness itself becomes the pickup
   * and boarding reason - strictly inside combat-free, threat-free
   * idleness, never a reason to leave a fight.
   */
  def pairIdle(shuttle: FriendlyUnitInfo, reaver: FriendlyUnitInfo): Boolean =
    With.geography.ourBases.isEmpty &&
      ! reaver.loaded &&
      reaver.matchups.targets.isEmpty &&
      shuttle.matchups.threats.isEmpty

  /**
   * Duelist target on baseless maps (UMS duels itinerary no bases, so no
   * mission forms and no army exists to support): nearest known enemy
   * pixel. With bases the pair never hunts alone like this.
   */
  def duelTarget(from: Pixel): Option[Pixel] =
    Maff.minBy(With.units.enemy.view.filter(e => e.alive && e.likelyStillThere))(_.pixelDistanceCenter(from)).map(_.pixel)

  private def health01(unit: FriendlyUnitInfo): Double =
    1.0 - unit.woundPercent()

  /**
   * Raids are a Terran/Protoss tool: against Zerg every pair supports the
   * army, so the doctrine lives here with the other pair reads, not inside
   * the status mapper. One read per frame for every caller.
   */
  private val enemyIsZergCache = new Performance.Cache(() => With.enemies.exists(_.isZerg))

  private def status(facts: => ReaverShuttleStatus.PairStatus): ReaverShuttleStatus.PairStatus =
    if (enemyIsZergCache()) ReaverShuttleStatus.SupportArmy else facts

  /** Shared status for the Shuttle's pair (empty ride supports the army). */
  def statusOfShuttle(shuttle: FriendlyUnitInfo): ReaverShuttleStatus.PairStatus = {
    val passenger = passengerOf(shuttle)
    status(ReaverShuttleStatus.decide(
      isOldestReaver  = passenger.exists(isOldest),
      shuttleHealth01 = health01(shuttle),
      reaverHealth01  = passenger.map(health01).getOrElse(1.0)))
  }

  /** Shared status for the Reaver's pair (no ride supports the army). */
  def statusOfReaver(reaver: FriendlyUnitInfo): ReaverShuttleStatus.PairStatus = {
    val ride = rideOf(reaver)
    status(ReaverShuttleStatus.decide(
      isOldestReaver  = isOldest(reaver),
      shuttleHealth01 = ride.map(health01).getOrElse(1.0),
      reaverHealth01  = health01(reaver)))
  }

  /** The army to follow: the attack squad's goal, or home. */
  def armyGoal: Pixel =
    With.tactics.attackSquad.vicinity

  /** The raid goal: the squad's target, or the nearest enemy base, or - baseless - the nearest known enemy. */
  def raidGoal(unit: FriendlyUnitInfo): Pixel =
    unit.squad.map(_.vicinity).getOrElse(
      Maff.minBy(With.geography.enemyBases)(b => unit.pixelDistanceCenter(b.heart.center))
        .map(_.heart.center).getOrElse(duelTarget(unit.pixel).getOrElse(unit.pixel)))

  /**
   * A bait ride: an empty hull with no Reaver or High Templar anywhere -
   * nothing to ferry, so it provokes the enemy army instead of hovering
   * over ours.
   */
  def isBaitRide(shuttle: FriendlyUnitInfo): Boolean =
    shuttle.loadedUnitCount == 0 && ! anyPairCargo()

  /** Where a bait ride loiters: the known enemy main, else the enemy start. */
  def baitGoal: Pixel =
    With.scouting.enemyMain.map(_.heart.center).getOrElse(With.scouting.enemyHome.center)
}
