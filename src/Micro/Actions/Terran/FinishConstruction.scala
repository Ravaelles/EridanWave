package Micro.Actions.Terran

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object FinishConstruction extends Action {
  override def isContainer: Boolean = true
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = {
    unit.intent.toFinish.isDefined
  }

  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    Commander.rightClick(unit, unit.intent.toFinish.get)
    UnitControl.claimedIf(! unit.ready)
  }
}
