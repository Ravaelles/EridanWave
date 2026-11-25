package Micro.Actions.Scouting

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Actions.Scouting.Defense._
import Micro.Actions.Scouting.Harass._
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Scout extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = unit.agent.isScout
  
  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    CannonRush(unit)
    PylonTrap(unit)
    FightWithZealot(unit)
    DroneWarfare(unit)
    Harass4PoolWorkers(unit)
    SabotageProxy(unit)
    KnockKnock(unit)
    PreserveScout(unit)
    AttackBuilder(unit)
    BlockConstruction(unit)
    Search(unit)
    SearchWhenBored(unit)
    Commander.move(unit)
    UnitControl.claimedIf(! unit.ready)
  }
}
