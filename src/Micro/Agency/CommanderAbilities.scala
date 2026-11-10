package Micro.Agency

import Lifecycle.With
import Mathematics.Points.{Pixel, Points, Tile}
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/** See Commander: role-grouped delegates behind the facade. */
object CommanderAbilities {

  private[Micro] def buildScarab(unit: FriendlyUnitInfo, count: Int = 1): Unit = {

    if (unit.unready) return



    var queued = 0

    while (queued < count && unit.bwapiUnit.build(Protoss.Scarab.bwapiType)) queued += 1

    if (queued > 0) CommanderCore.sleep(unit)

  }



  private[Micro] def buildInterceptor(unit: FriendlyUnitInfo): Unit = {

    if (unit.unready) return



    unit.bwapiUnit.build(Protoss.Interceptor.bwapiType)

    CommanderCore.sleep(unit)

  }




  private[Micro] def cloak(unit: FriendlyUnitInfo, tech: Tech): Unit = {

    CommanderCore.leadFollower(unit, cloak(_, tech))

    if (unit.unready) return



    unit.agent.lastCloak = With.frame

    unit.bwapiUnit.cloak()

    CommanderCore.sleep(unit)

  }



  private[Micro] def decloak(unit: FriendlyUnitInfo, tech: Tech): Unit = {

    CommanderCore.leadFollower(unit, decloak(_, tech))

    if (unit.unready) return



    unit.bwapiUnit.decloak()

    CommanderCore.sleep(unit)

  }




  private[Micro] def burrow(unit: FriendlyUnitInfo): Unit = {

    if (unit.unready) return



    unit.bwapiUnit.burrow()

    CommanderCore.sleep(unit)

  }



  private[Micro] def unburrow(unit: FriendlyUnitInfo): Unit = {

    if (unit.unready) return



    unit.bwapiUnit.unburrow()

    CommanderCore.sleep(unit)

  }




  private[Micro] def lift(unit: FriendlyUnitInfo): Unit = {

    if (unit.unready) return



    unit.bwapiUnit.lift()

    CommanderCore.sleep(unit)

  }



  private[Micro] def land(unit: FriendlyUnitInfo, tile: Tile): Unit = {

    if (unit.unready) return



    val destination = unit.unitClass.tileArea.add(tile).center

    if (unit.pixelDistanceCenter(destination) > 2) {

      unit.bwapiUnit.move(destination.bwapi)

    } else {

      unit.bwapiUnit.land(tile.bwapi)

    }

    CommanderCore.sleep(unit)

  }



}
