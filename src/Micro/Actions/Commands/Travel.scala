package Micro.Actions.Commands

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Travel extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true
  override def allowed(unit: FriendlyUnitInfo): Boolean = unit.canMove
  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    if (unit.intent.canSneak) {
      MicroPathing.tryMovingAlongTilePath(unit, MicroPathing.getSneakyPath(unit))

      return UnitControl.claimedIf(! unit.ready)
    }

    Commander.move(unit)

    UnitControl.claimedIf(! unit.ready)
  }
}
