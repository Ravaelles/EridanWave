package Micro.Actions.Protoss.Reaver

import Micro.Actions.Action
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/** Entry to the Atlantis chain; the brain owns the order. */
object BeReaverAtlantis extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.Reaver(unit)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    ReaverAtlantisBrain.act(unit)
  }
}
