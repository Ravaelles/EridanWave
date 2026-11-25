package Micro.Actions.Terran

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Land extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true

  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    unit.unitClass.isFlyingBuilding
    && unit.flying
    && unit.squadIntent.toLand.isDefined)

  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    unit.squadIntent.toLand.foreach(Commander.land(unit, _))
    UnitControl.claimedIf(! unit.ready)
  }
}
