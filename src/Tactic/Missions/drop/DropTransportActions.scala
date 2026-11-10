package Tactic.Missions.drop

import Tactic.Missions.core.MissionDrop
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.{Action, Idle}
import Micro.Actions.Basic.DoNothing
import Micro.Actions.Combat.Maneuvering.Retreat
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Time.Seconds

/**
 * Transport micro actions: assemble, travel, land, raid and evacuate.
 * Each reads live state (routes, raid decisions) before issuing orders
 * because squad updates run slower than micro frames.
 */
trait DropTransportActions { self: MissionDrop =>

  object ActionAssembleTransport extends Action {
    override protected def perform(transport: FriendlyUnitInfo): Unit = {
      accountForCargo(transport)
      transport.agent.passengers.filterNot(passengers.contains).foreach(transport.agent.removePassenger)
      if (unassignedCargo(transport).nonEmpty) {
        if (!unloadUnassignedCargo(transport)) ActionTravelTransport.delegate(transport)

        return
      }

      val pickup = pickupTarget(transport, assembling = true)
      if (pickup.isEmpty) return Commander.hold(transport)

      if (transport.matchups.withinSafetyMargin) {
        val toPickup = pickup.get
        transport.agent.decision.set(toPickup.pixel)
        Commander.rightClick(transport, toPickup)
      } else {
        Retreat.delegate(transport)
      }
    }
  }

  object ActionTravelTransport extends Action {
    override protected def perform(transport: FriendlyUnitInfo): Unit = {
      if (state == Assembling && unloadUnassignedCargo(transport)) return

      accountForCargo(transport)
      With.grids.enemyRangeAir.update()
      val recalled = divertIfRecalled(transport)
      if ( ! recalled && handoffToRaid(transport)) return

      finishWithdrawal()
      if (transport.intent.action != this) {
        transport.agent.resetDestinations()
        transport.intent.action.delegate(transport)

        return
      }

      advanceOrHold(transport)
    }

    // Squad updates are slower than micro. Recheck a recall here, before
    // issuing another movement command. Falls through like the old chain:
    // evacuation sets intents the steps below then carry out.
    private def divertIfRecalled(transport: FriendlyUnitInfo): Boolean = {
      val recalled = (state == Travelling || state == Landing) && (shouldGoHome || rejectApproach(transport) ||
        destinationBase.exists(b => !b.isEnemy && targetCleared(b)))
      if (recalled) {
        if (!shouldGoHome && itinerary.headOption == destinationBase && itinerary.nonEmpty) itinerary.dequeue()
        updateState(goHome = shouldGoHome)
        if (state == Evacuating) evacuate() else if (state == Escaping) escape() else travel()
      }
      recalled
    }

    private def handoffToRaid(transport: FriendlyUnitInfo): Boolean = {
      val ready = defaultCombatLanding && (state == Travelling || state == Landing) && readyToRaid(transport)
      if (ready) {
        raidingTransports += transport
        onRaidHandoff(transport)
        if (transports.forall(t => raidingTransports(t) || !passengersFor(t).exists(requiredPassenger))) transition(Raiding)
        else transition(Landing)
        land()
        transport.agent.resetDestinations()
        raidTransportAction.delegate(transport)
      }
      ready
    }

    private def advanceOrHold(transport: FriendlyUnitInfo): Unit = {
      val destination = transport.intent.terminus.getOrElse(transport.agent.defaultHome).tile
      val waypoint = routeWaypoint(transport, destination)
      if (!threatenedOnApproach(transport, waypoint) && waypoint.nonEmpty) {
        transport.agent.decision.set(waypoint.get)
        Commander.move(transport)
      } else {
        if (routes.get(transport).exists(_.stalled)) {
          if (state == Travelling || state == Landing) {
            if (passengersFor(transport).exists(requiredPassenger) && itinerary.headOption == destinationBase && itinerary.nonEmpty) itinerary.dequeue()
          } else if (state != Raiding) returningHome = true
        }
        transport.agent.redoubt.set(transport.agent.defaultHome)
        Retreat.delegate(transport)
        Commander.hold(transport)
      }
    }
  }

  object ActionLandTransport extends Action {
    override protected def perform(transport: FriendlyUnitInfo): Unit = {
      if (passengersFor(transport).forall(initiallyLanded.contains)) {
        raidTransportAction.delegate(transport)

        return
      }

      val runway = destinationBase.map(_.heart.center).getOrElse(vicinity)
      val droppables = transport.loadedUnits.filterNot(initiallyLanded.contains)
        .filter(passenger =>
          transport.doomedInFrames < transport.framesToTravelTo(runway) + Seconds(3)()
          || passenger.pixelDistanceCenter(runway) <= Math.max(64, passenger.effectiveRangePixels - 96)
          || (passenger.pixelDistanceCenter(runway) < passenger.effectiveRangePixels
            && passenger.matchups.threats.exists(t => t.inRangeToAttack(passenger, runway) && ! t.inRangeToAttack(passenger))))
        .sortBy(_.subjectiveValue * (if (transport.tile.enemyRange > 0) -1 else 1))
      droppables.headOption.foreach(Commander.unload(transport, _))
      transport.agent.decision.set(runway)
      Commander.move(transport)
    }
  }

  object ActionRaidTransport extends Action {
    override protected def perform(transport: FriendlyUnitInfo): Unit = {
      val crew = passengersFor(transport)
      if (crew.isEmpty) Commander.hold(transport) else {
        transport.agent.decision.set(Maff.centroid(Maff.orElse(crew.view.filterNot(_.loaded), crew.view).map(_.pixel)))
        Commander.move(transport)
      }
    }
  }

  object ActionEvacuateTransport extends Action {
    override protected def perform(transport: FriendlyUnitInfo): Unit = {
      val pickup = pickupTarget(transport, assembling = false)
      if (pickup.isEmpty) {
        if (transport.matchups.threats.nonEmpty && !transport.matchups.withinSafetyMargin) Retreat.delegate(transport)
        Commander.hold(transport)
      } else if (transport.pixelDistanceCenter(pickup.get) > 384) {
        intendRoute(transport, pickup.get.tile)
        ActionTravelTransport.delegate(transport)
      } else {
        // A nearby rescue takes priority over the Shuttle's own safety margin.
        Commander.rightClick(transport, pickup.get)
      }
    }
  }
}
