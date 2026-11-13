package Micro.Actions.Protoss.Shuttle

import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Hitchhiker ditch (plain Boolean guard for the BeShuttle chain): a Shuttle
 * carrying anyone but its Reavers and Templars drops them. Returns whether
 * anyone was dropped, so the chain stops for the frame instead of questing
 * on top of the unload.
 */
object ShuttleDitchPassengers {

  def ditch(shuttle: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Shuttle(shuttle)) return false
    if (shuttle.base.exists(_.owner.isEnemy) && ! shuttle.agent.destinationFinal().base.exists(_.owner.isEnemy)) return false

    val hitchhikers = (shuttle.agent.passengers ++ shuttle.loadedUnits)
      .distinct
      .filter(p => p.isNone(Protoss.Reaver, Protoss.HighTemplar) && ! p.squad.exists(shuttle.squad.contains))
      .toVector
    if (hitchhikers.isEmpty) return false

    hitchhikers.foreach(shuttle.agent.removePassenger)
    hitchhikers.foreach(Commander.unload(shuttle, _))
    shuttle.agent.act("ShuttleDitchPassengers")

    true
  }
}
