package Micro.Actions.Commands

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Attack extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true
  override def allowed(unit: FriendlyUnitInfo): Boolean =
    unit.canAttack && unit.agent.toAttack.isDefined
  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    Commander.attack(unit)
    UnitControl.claimedIf(! unit.ready)
  }
}
