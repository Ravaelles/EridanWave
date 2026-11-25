package Micro.Actions.Basic

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Unstick extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    unit.canMove
    && ! unit.flying
    && ! unit.loaded
    && ! unit.unitClass.floats
    && ! Protoss.Reaver(unit)
    && unit.canAttack
    && unit.seeminglyStuck
  )
  
  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    Commander.stop(unit)
    UnitControl.Claimed
  }
}
