package Tactic.Missions.drop

import Micro.Actions.UnitControl
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
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    override protected def perform(passenger: FriendlyUnitInfo): UnitControl = {
      if (passenger.loaded) return UnitControl.Declined

      passenger.agent.redoubt.set(
        passenger.agent.decision.set(assignedTransport(passenger).map(_.pixel)))
      ReloadScarabs.delegate(passenger)
      if (readyForPickup(passenger)) {
        boardSelected(passenger, assembling = true)
      }
      UnitControl.claimedIf(! passenger.ready)
    }
  }

  object ActionLandPassenger extends Action {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
      if (!unit.loaded || initiallyLanded(unit)) Idle.delegate(unit)
      UnitControl.claimedIf(! unit.ready)
    }
  }

  object ActionEvacuatePassenger extends Action {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    override protected def perform(passenger: FriendlyUnitInfo): UnitControl = {
      if (passenger.loaded) return UnitControl.Declined

      boardSelected(passenger, assembling = false)
      UnitControl.claimedIf(! passenger.ready)
    }
  }
}
