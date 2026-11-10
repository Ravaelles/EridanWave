package Tactic.Missions.drop

import Tactic.Missions.core.MissionDrop
import Tactic.Missions.routing.DropManifest
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Basic.DoNothing
import Micro.Actions.Idle
import Micro.Agency.Commander
import Planning.ResourceLocks.LockUnits
import ProxyBwapi.Orders
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.UnitCounters.CountOne

/**
 * Drop manifest bookkeeping: who rides which transport, boarding the
 * selected passenger and unloading stray cargo.
 */
trait DropManifestOps { self: MissionDrop =>

  private[Missions] def unassignedCargo(transport: FriendlyUnitInfo): Seq[FriendlyUnitInfo] = transport.loadedUnits.filter(p => p.alive && !passengers.contains(p))
  private[Missions] def allAboard: Boolean = passengers.forall(_.transport.exists(transports.contains)) && transports.forall(unassignedCargo(_).isEmpty)
  private[Missions] def allLanded: Boolean = passengers.forall(initiallyLanded.contains)
  private[Missions] def allHome: Boolean = units.forall(_.base.exists(_.owner.isUs))

  // Keep Reavers already aboard during assembly, even above the recruitment cap.
  // Any passenger carried into a raid must remain part of the recovery plan.
  // Public: the Transport action also accounts for stray cargo.
  final def accountForCargo(transport: FriendlyUnitInfo): Unit = {
    if (!transports.contains(transport)) return

    val cargo = unassignedCargo(transport).filter(p => state != Assembling || Protoss.Reaver(p)).toVector
    if (cargo.isEmpty) return

    passengers ++= cargo
    With.recruiter.lockTo(cargoLock, cargo)
    cargo.foreach(p => {
      With.logger.debug(s"$this retaining carried passenger $p aboard $transport")
      p.setSquad(Some(this))
      transport.agent.addPassenger(p)
      p.intend(this).setTerminus(vicinity).setAction(if (state == Raiding || raidingTransports(transport)) Idle else DoNothing)
    })
  }

  private[Missions] def updateManifest(): Unit = {
    transports.foreach(accountForCargo)
    val current = passengers.flatMap(p => assignedTransport(p).map(p -> _)).toMap
    val assigned = DropManifest.fill(transports, passengers, current)
    transports.foreach(t => t.agent.passengers.filterNot(p => assigned.get(p).contains(t)).foreach(t.agent.removePassenger))
    assigned.foreach { case (p, t) => if (!p.agent.ride.contains(t)) t.agent.addPassenger(p) }
    releasePassengers(passengers.filterNot(assigned.contains).toSet)
    releaseContainReavers()
    routes.keys.filterNot(transports.contains).toVector.foreach(routes.remove)
    pickupTargets.keys.filterNot(transports.contains).toVector.foreach(pickupTargets.remove)
    raidingTransports.retain(transports.contains)
  }

  /** Contain vs Zerg drafts every mission Reaver to the break; other
   * races keep one crew while the rest stay mission-free. */
  private[Missions] def releaseContainReavers(): Unit = {
    if ( ! Micro.Actions.Protoss.Reaver.ContainStatus.now()) return

    if ( ! Micro.Actions.Protoss.Reaver.ReaverContain.recallAll(Micro.Actions.Protoss.Reaver.ContainStatus.vsZerg())) return

    releasePassengers(passengers.filter(Protoss.Reaver).toSet)
  }

  private[Missions] def releasePassengers(stranded: Set[FriendlyUnitInfo]): Unit = {
    if (stranded.nonEmpty) {
      // Losing capacity must not leave evacuation waiting forever for an impossible load.
      With.recruiter.locksOf(this).toVector.foreach(lock => {
        val retained = lock.units.filterNot(stranded).toVector
        if (retained.size != lock.units.size) {
          lock.release()
          With.recruiter.lockTo(lock, retained)
        }
      })
      stranded.foreach(p => {
        if (!p.loaded) Commander.stop(p)
        p.agent.ride.foreach(_.agent.removePassenger(p))
        p.agent.commit = false
        p.agent.toBoard = None
        p.agent.wantsPickup = false
        p.setSquad(None)
        val intent = p.intend(this).setAction(Idle).setTerminus(p.agent.defaultHome).setStation(None).setRedoubt(None).setBoard(None).setAttack(None)
        intent.targets = None
        p.agent.resetDestinations()
      })
      passengers --= stranded
    }
  }

  private[Missions] def readyForPickup(passenger: FriendlyUnitInfo): Boolean =
    !Protoss.Reaver(passenger) || passenger.scarabs + passenger.trainingQueue.size >= 2

  private[Missions] def pickupTarget(transport: FriendlyUnitInfo, assembling: Boolean): Option[FriendlyUnitInfo] = pickupTargets.synchronized {
    abandonUnsafePickups()
    if (assembling && unassignedCargo(transport).nonEmpty) return None

    val unloaded = passengersFor(transport).filterNot(_.loaded)
      .filter(p => !assembling || readyForPickup(p))
    val required = Maff.orElse(unloaded.filter(requiredPassenger), unloaded).toVector
    val candidates = if (assembling) required else
      Maff.orElse(required.filter(_.matchups.threatsInRange.nonEmpty), required).toVector
    val selected = pickupTargets.get(transport).filter(candidates.contains)
      .orElse(Maff.minBy(candidates)(_.pixelDistanceSquared(transport)))
    selected.foreach(pickupTargets(transport) = _)
    selected
  }

  private[Missions] def boardSelected(passenger: FriendlyUnitInfo, assembling: Boolean): Unit = {
    abandonUnsafePickups()
    assignedTransport(passenger).foreach(t => {
      if (pickupTarget(t, assembling).contains(passenger)) Commander.rightClick(passenger, t)
      else if (passenger.order == Orders.EnterTransport) Commander.stop(passenger)
      else DoNothing.delegate(passenger)
    })
  }

  private[Missions] def unloadUnassignedCargo(transport: FriendlyUnitInfo): Boolean = {
    accountForCargo(transport)
    val cargo = unassignedCargo(transport).headOption
    if (cargo.isEmpty) return false

    if (!transport.base.exists(_.owner.isUs) || !transport.tile.adjacent9.exists(_.walkable)) {
      intendRoute(transport, transport.agent.defaultHome.tile)

      return false
    }
    // Keep unload and movement in the same micro controller. A squad-issued
    // unload was immediately overwritten by assembly's subsequent orders.
    Commander.unload(transport, cargo.get)
    Commander.sleep(transport, 8)
    true
  }
}
