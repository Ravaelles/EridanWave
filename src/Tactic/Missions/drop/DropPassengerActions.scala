package Tactic.Missions.drop

import Tactic.Missions.core.MissionDrop
import Micro.Actions.{Action, Idle}
import Micro.Actions.Basic.DoNothing
import Micro.Actions.Protoss.Support.ReloadScarabs
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Passenger micro actions: board during assembly, ride out the landing,
 * board again on evacuation.
 */
trait DropPassengerActions { self: MissionDrop =>

  object ActionAssemblePassenger extends Action {
    override protected def perform(passenger: FriendlyUnitInfo): Unit = {
      if (passenger.loaded) return

      passenger.agent.redoubt.set(
        passenger.agent.decision.set(assignedTransport(passenger).map(_.pixel)))
      ReloadScarabs.delegate(passenger)
      if (readyForPickup(passenger)) {
        boardSelected(passenger, assembling = true)
      }
    }
  }

  object ActionLandPassenger extends Action {
    override protected def perform(unit: FriendlyUnitInfo): Unit = {
      if (!unit.loaded || initiallyLanded(unit)) Idle.delegate(unit)
    }
  }

  object ActionEvacuatePassenger extends Action {
    override protected def perform(passenger: FriendlyUnitInfo): Unit = {
      if (passenger.loaded) return

      boardSelected(passenger, assembling = false)
    }
  }
}
