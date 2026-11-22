package Micro.Actions.Terran

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Land extends Action {
  override def isContainer: Boolean = true

  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    unit.unitClass.isFlyingBuilding
    && unit.flying
    && unit.intent.toLand.isDefined)

  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    unit.intent.toLand.foreach(Commander.land(unit, _))
    UnitControl.claimedIf(! unit.ready)
  }
}
