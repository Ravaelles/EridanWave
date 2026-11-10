package Tactic.Missions.drop

import Tactic.Missions.core.MissionDrop
import Micro.Actions.Basic.DoNothing
import Micro.Actions.{Action, Idle}
import Tactic.Squads.SquadAutomation

/**
 * Drop phase bodies: what transports and passengers do in each state.
 * The raid phase stays in the class: drops override it per payload.
 */
trait DropPhases { self: MissionDrop =>

  private[Missions] def assemble(): Unit = {
    transports.foreach(t => {
      val pickup = pickupTarget(t, assembling = true)
      if (unassignedCargo(t).nonEmpty) t.intend(this).setAction(ActionAssembleTransport)
      else if (pickup.exists(t.pixelDistanceCenter(_) > 384)) intendRoute(t, pickup.get.tile)
      else t.intend(this).setAction(ActionAssembleTransport)
    })
    passengers.foreach(_.intend(this).setAction(ActionAssemblePassenger))
  }

  private[Missions] def travel(): Unit = {
    transports.foreach(t => intendRoute(t, vicinity.tile))
    passengers.foreach(_.intend(this).setAction(DoNothing))
  }

  private[Missions] def land(): Unit = {
    SquadAutomation.targetRaid(this)
    passengers.foreach(_.agent.commit = commitWhileRaiding)
    passengers.foreach(_.intend(this).setTerminus(vicinity))
    transports.foreach(t => {
      if (defaultCombatLanding && raidingTransports(t)) t.intend(this).setTerminus(vicinity).setAction(raidTransportAction)
      else if (defaultCombatLanding && !passengersFor(t).exists(requiredPassenger)) intendRoute(t, t.agent.defaultHome.tile)
      else if (defaultCombatLanding || !t.base.exists(destinationBase.contains)) intendRoute(t, landingGoal(t).tile)
      else t.intend(this).setAction(ActionLandTransport)
    })
    passengers.foreach(p => p.intend(this).setAction(
      if (!defaultCombatLanding) ActionLandPassenger
      else if (assignedTransport(p).exists(raidingTransports)) Idle else DoNothing))
  }

  private[Missions] def evacuate(): Unit = {
    passengers.foreach(_.agent.commit = false)
    transports.foreach(t => {
      val pickup = pickupTarget(t, assembling = false)
      if (pickup.exists(t.pixelDistanceCenter(_) > 384)) intendRoute(t, pickup.get.tile)
      else t.intend(this).setAction(ActionEvacuateTransport)
    })
    passengers.foreach(_.intend(this).setAction(ActionEvacuatePassenger))
  }

  private[Missions] def escape(): Unit = {
    passengers.foreach(_.agent.commit = false)
    transports.foreach(t => intendRoute(t, t.agent.defaultHome.tile))
    passengers.foreach(_.intend(this).setAction(DoNothing))
  }
}
