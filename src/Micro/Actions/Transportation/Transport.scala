package Micro.Actions.Transportation

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Actions.Protoss.ReaverShuttle.BeShuttle
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Transport extends Action {
  override def isContainer: Boolean = true
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = unit.isTransport
  
  override protected def perform(transport: FriendlyUnitInfo): UnitControl = {
    transport.squad.foreach(squad => {
      squad.accountForCargo(transport)
      squad.abandonUnsafePickups()
    })
    val passengers = transport.agent.passengers
    passengers.indices.foreach(i => {
      // Don't get stuck trying to unload a passenger that fails to land
      // and do save some frames for microing the transport itself
      if (i == transport.agent.run % (passengers.length + 1) && transport.tile.adjacent9.exists(_.walkable)
        && (passengers(i).agent.wantsUnload || transport.squad.exists(_.requestsUnload(passengers(i))))) {
        Commander.unload(transport, passengers(i))
      }
    })

    Evacuate(transport)
    if (transport.unready) return UnitControl.Claimed // Evacuate already moved

    BeShuttle(transport)
    UnitControl.claimedIf(! transport.ready)
  }
}
