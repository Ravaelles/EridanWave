package Micro.Actions.Protoss.ReaverShuttle

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

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

  private def health01(unit: FriendlyUnitInfo): Double =
    1.0 - unit.woundPercent()

  /** Shared status for the Shuttle's pair (empty ride supports the army). */
  def statusOfShuttle(shuttle: FriendlyUnitInfo): ReaverShuttleStatus.PairStatus = {
    val passenger = passengerOf(shuttle)
    ReaverShuttleStatus.decide(
      isOldestReaver  = passenger.exists(isOldest),
      shuttleHealth01 = health01(shuttle),
      reaverHealth01  = passenger.map(health01).getOrElse(1.0))
  }

  /** Shared status for the Reaver's pair (no ride supports the army). */
  def statusOfReaver(reaver: FriendlyUnitInfo): ReaverShuttleStatus.PairStatus = {
    val ride = rideOf(reaver)
    ReaverShuttleStatus.decide(
      isOldestReaver  = isOldest(reaver),
      shuttleHealth01 = ride.map(health01).getOrElse(1.0),
      reaverHealth01  = health01(reaver))
  }

  /** The army to follow: the attack squad's goal, or home. */
  def armyGoal: Pixel =
    With.tactics.attackSquad.vicinity

  /** The raid goal: the squad's target, or the nearest enemy base. */
  def raidGoal(unit: FriendlyUnitInfo): Pixel =
    unit.squad.map(_.vicinity).getOrElse(
      Maff.minBy(With.geography.enemyBases)(b => unit.pixelDistanceCenter(b.heart.center))
        .map(_.heart.center).getOrElse(unit.pixel))
}
