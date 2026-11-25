package Micro.Actions.Basic

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object DoNothing extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true
  override protected def perform(unit: FriendlyUnitInfo): UnitControl = { Commander.sleep(unit, 1); UnitControl.Claimed }
}
