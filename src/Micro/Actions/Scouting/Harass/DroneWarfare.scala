package Micro.Actions.Scouting.Harass

import Micro.Actions.UnitControl
import Lifecycle.With
import Micro.Actions.Action
import Micro.Agency.Commander
import Micro.Targeting.Target
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object DroneWarfare extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true

  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    unit.intent.toScoutTiles.nonEmpty
    && unit.intent.canFight
    && With.blackboard.droneWarfare())

  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    Target.choose(unit)
    Commander.attack(unit)
    UnitControl.claimedIf(! unit.ready)
  }
}
