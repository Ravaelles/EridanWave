package Micro.Agency

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Transportation.DropSafety
import ProxyBwapi.Orders
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/** See Commander: role-grouped delegates behind the facade. */
object CommanderTransport {

  private[Micro] def rightClick(unit: FriendlyUnitInfo, target: UnitInfo): Unit = {

    // Boarding from either side can pull the Shuttle into range between mission updates.

    val pickup = target.friendly.flatMap(other =>

      if (unit.isTransport) Some((unit, other)) else if (other.isTransport) Some((other, unit)) else None)

    if (pickup.exists { case (transport, passenger) => transport.squad.exists {

      case mission: Tactic.Missions.core.MissionDrop if !mission.allowsPickup(transport, passenger) =>

        mission.abandonUnsafePickups()

        true

      case _ => false

    }}) {

      CommanderMove.stop(unit)



      return

    }



    CommanderCore.leadFollower(unit, rightClick(_, target))

    unit.agent.tryingToMove = unit.pixelDistanceEdge(target) > CommanderCore.tryingToMoveThreshold

    if (unit.agent.ride.contains(target)) {

      unit.agent.wantsPickup = true

    } else {

      unit.agent.setRideGoal(target.pixel)

    }

    if (unit.unready) return

    // EnterTransport also orders the Shuttle to fly to this passenger. Keep long

    // approaches routed, while allowing nearby rescues to interrupt a retreat.

    if (target.friendly.exists(t => Protoss.Shuttle(t) && unit.isOurs && !unit.flying && unit.pixelDistanceCenter(t) > 384)) {

      if (unit.order == Orders.EnterTransport) CommanderMove.stop(unit) else CommanderMove.doNothing(unit)



      return

    }



    if ( ! Zerg.Lurker(unit) && CommanderCore.autoUnburrow(unit)) return



    unit.bwapiUnit.rightClick(target.bwapiUnit)

    CommanderCore.sleepAttack(unit)

  }




  private[Micro] def requestUnload(passenger: FriendlyUnitInfo): Unit = {

    passenger.agent.wantsUnload = true

  }



  private[Micro] def unload(transport: FriendlyUnitInfo, passenger: UnitInfo): Unit = {

    // No sleeping required

    // Never drop into static defense: arrival and engage paths unload
    // wherever the ride stops, so the veto lives here and covers them all.
    // Bunkers at home are unaffected - no enemy buildings stand near them.
    if (transport.flying) {
      val nearestStatic = Maff.minBy(With.units.enemy.filter(e =>
        e.unitClass.isCombatBuilding && e.canAttackGround && e.likelyStillThere))(_.tileDistanceEdge(transport))
      if (DropSafety.vetoLanding(nearestStatic.map(_.tileDistanceEdge(transport)))) return
    }

    transport.bwapiUnit.unload(passenger.bwapiUnit)

  }



}
