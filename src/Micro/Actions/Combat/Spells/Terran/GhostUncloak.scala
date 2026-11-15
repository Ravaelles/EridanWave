package Micro.Actions.Combat.Spells.Terran

import Lifecycle.With
import Micro.Actions.Action
import Micro.Agency.Commander
import Micro.Actions.Combat.Spells.Spell
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

object GhostUncloak extends Action with Spell {

  def isCaster(unit: UnitInfo): Boolean = Terran.Ghost(unit)

  def canCast(unit: UnitInfo): Boolean = Terran.Ghost(unit) && unit.cloaked
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = {
    unit.is(Terran.Ghost)                                     &&
    unit.cloaked                                              &&
    ! unit.matchups.enemies.exists(_.unitClass.attacksGround) &&
    With.framesSince(unit.agent.lastCloak) > 24 * 4
  }
  
  override protected def perform(unit: FriendlyUnitInfo) {
    Commander.decloak(unit, Terran.GhostCloak)
  }
}
