package Micro.Actions.Basic

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Bunk extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = {
    unit.agent.toBoard.exists(Terran.Bunker)
  }
  
  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    if (unit.loaded) {
      if (unit.transport == unit.agent.toBoard) {
        Commander.doNothing(unit)
      } else {
        unit.transport.foreach(Commander.unload(_, unit))
      }
    } else {
      Commander.rightClick(unit, unit.agent.toBoard.get)
    }
    UnitControl.claimedIf(! unit.ready)
  }
}
