package Micro.Actions.Combat.Tactics.Potshots

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Targeting.Target
import Micro.Agency.Commander
import Micro.Targeting.FiltersSituational.TargetFilterPotshot
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Potshot extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    (unit.tile.enemyVulnerabilityGround > 0 || unit.canAttackAir)
    && unit.tile.enemyRangeAgainst(unit) > 0
    && unit.readyForAttackOrder)
  
  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    Target.choose(unit, TargetFilterPotshot)
    Commander.attack(unit)
    UnitControl.claimedIf(! unit.ready)
  }
}
