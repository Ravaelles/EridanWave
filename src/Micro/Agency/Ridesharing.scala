package Micro.Agency

import Mathematics.Points.Pixel
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

import scala.collection.mutable.ArrayBuffer

/**
 * Ridesharing (extracted from Agent): which passengers this transport
 * carries and which transport this unit rides. One reason to change:
 * boarding state. Agent keeps one-line delegates so call sites do not
 * move; the synchronized reservation lives here, not in the god object.
 */
final class Ridesharing(val unit: FriendlyUnitInfo) {

  private val _passengers : ArrayBuffer[FriendlyUnitInfo] = new ArrayBuffer[FriendlyUnitInfo]
  private var _ride       : Option[FriendlyUnitInfo]      = None
  private var _rideGoal   : Option[Pixel]                 = None

  private def reservedPassengers: Vector[FriendlyUnitInfo] = _passengers.synchronized(_passengers.toVector)

  def passengers            : Seq[FriendlyUnitInfo]     = (reservedPassengers ++ unit.loadedUnits).distinct
  def passengersPrioritized : Seq[FriendlyUnitInfo]     = passengers.sortBy(p => - p.unitClass.subjectiveValue - p.energy / 250.0 + p.frameDiscovered / 1e6)
  def hailers               : Seq[FriendlyUnitInfo]     = passengers            .filter(p => ! p.loaded && p.agent.wantsPickup)
  def hailersPrioritized    : Seq[FriendlyUnitInfo]     = passengersPrioritized .filter(p => ! p.loaded && p.agent.wantsPickup)
  def passengerSize         : Int                       = passengers.view.map(_.unitClass.spaceRequired).sum
  def ride                  : Option[FriendlyUnitInfo]  = _ride
  def rideGoal              : Option[Pixel]             = _rideGoal
  def isPrimaryPassenger    : Boolean                   = ride.exists(_.agent.passengersPrioritized.headOption.contains(unit))

  def addPassenger(passenger: FriendlyUnitInfo): Unit = {
    if (passengerSize + passenger.unitClass.spaceRequired > 8 && ! passenger.transport.contains(unit)) return

    passenger.agent.ridesharing.unboard
    passenger.agent.ridesharing._ride = Some(unit)
    _passengers.synchronized {
      _passengers -= passenger
      _passengers += passenger
    }
  }

  def removePassenger(passenger: FriendlyUnitInfo): Unit = {
    passenger.agent.ridesharing._ride = passenger.agent.ridesharing._ride.filter(_ != unit)
    _passengers.synchronized {
      _passengers -= passenger
    }
  }

  def removeAllPassengers(): Unit = {
    val all = passengers.toVector
    all.foreach(removePassenger)
  }

  def setRideGoal(to: Pixel): Unit = {
    _rideGoal = Some(to)
  }

  def clearRideGoal(): Unit = {
    _rideGoal = None
  }

  /**
   * Frame reconciliation (called from Agent.execute): drop dead rides and
   * passengers, adopt engine-loaded hulls into the reservation. Runs every
   * frame, so boarding state can never desync from the engine.
   */
  def reconcileBoarding(): Unit = {
    clearRideGoal()
    ride.filterNot(_.alive).foreach(_.agent.removePassenger(unit))
    passengers.view.filter(u => ! u.alive || ! u.isOurs || u.unitClass.isBuilding).foreach(removePassenger)
    val reserved = reservedPassengers
    unit.loadedUnits.filterNot(reserved.contains).foreach(addPassenger)
  }

  private[Agency] def unboard: Unit = {
    _ride.foreach(_.agent.ridesharing.removePassenger(unit))
    _ride = None
  }
}
