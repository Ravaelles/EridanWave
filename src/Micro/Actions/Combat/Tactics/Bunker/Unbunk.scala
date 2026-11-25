package Micro.Actions.Combat.Tactics.Bunker

import Micro.Actions.UnitControl
import Lifecycle.With
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Unbunk extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    With.self.isTerran
    && ! unit.agent.toBoard.exists(unit.transport.contains)
    && unit.transport.exists(Terran.Bunker)
    && unit.transport.get.matchups.targetsInRange.isEmpty
    && unit.pixelDistanceTravelling(unit.agent.destinationNext()) > 32.0 * 7.0)
  
  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    Commander.unload(unit.transport.get, unit)
    UnitControl.claimedIf(! unit.ready)
  }
}
