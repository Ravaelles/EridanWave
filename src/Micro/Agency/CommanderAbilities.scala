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
    var issued = false
    while (queued < count && unit.bwapiUnit.build(Protoss.Scarab.bwapiType)) {
      issued = true
      queued += 1
    }
    // Claim after the loop on purpose: the loop may issue nothing (full
    // queue), and a claim without an issuance would mask the frame's real
    // first order as a duplicate. Same-frame attribution stays correct.
    if (issued) CommanderCore.claimOrder(unit, "CommanderAbilities.buildScarab")
    if (queued > 0) CommanderCore.sleep(unit)
  }
  private[Micro] def buildInterceptor(unit: FriendlyUnitInfo): Unit = {
    if (unit.unready) return

    CommanderCore.claimOrder(unit, "CommanderAbilities.buildInterceptor")
    unit.bwapiUnit.build(Protoss.Interceptor.bwapiType)
    CommanderCore.sleep(unit)
  }
  private[Micro] def cloak(unit: FriendlyUnitInfo, tech: Tech): Unit = {
    CommanderCore.leadFollower(unit, cloak(_, tech))
    if (unit.unready) return

    unit.agent.lastCloak = With.frame
    CommanderCore.claimOrder(unit, "CommanderAbilities.cloak")
    unit.bwapiUnit.cloak()
    CommanderCore.sleep(unit)
  }
  private[Micro] def decloak(unit: FriendlyUnitInfo, tech: Tech): Unit = {
    CommanderCore.leadFollower(unit, decloak(_, tech))
    if (unit.unready) return

    CommanderCore.claimOrder(unit, "CommanderAbilities.decloak")
    unit.bwapiUnit.decloak()
    CommanderCore.sleep(unit)
  }
  private[Micro] def burrow(unit: FriendlyUnitInfo): Unit = {
    if (unit.unready) return

    CommanderCore.claimOrder(unit, "CommanderAbilities.burrow")
    unit.bwapiUnit.burrow()
    CommanderCore.sleep(unit)
  }
  private[Micro] def unburrow(unit: FriendlyUnitInfo): Unit = {
    if (unit.unready) return

    CommanderCore.claimOrder(unit, "CommanderAbilities.unburrow")
    unit.bwapiUnit.unburrow()
    CommanderCore.sleep(unit)
  }
  private[Micro] def lift(unit: FriendlyUnitInfo): Unit = {
    if (unit.unready) return

    CommanderCore.claimOrder(unit, "CommanderAbilities.lift")
    unit.bwapiUnit.lift()
    CommanderCore.sleep(unit)
  }
  private[Micro] def land(unit: FriendlyUnitInfo, tile: Tile): Unit = {
    if (unit.unready) return

    val destination = unit.unitClass.tileArea.add(tile).center
    if (unit.pixelDistanceCenter(destination) > 2) {
      CommanderCore.claimOrder(unit, "CommanderAbilities.land")
      unit.bwapiUnit.move(destination.bwapi)
    } else {
      CommanderCore.claimOrder(unit, "CommanderAbilities.land")
      unit.bwapiUnit.land(tile.bwapi)
    }
    CommanderCore.sleep(unit)
  }
}

