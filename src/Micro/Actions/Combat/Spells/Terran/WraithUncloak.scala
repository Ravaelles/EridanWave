package Micro.Actions.Combat.Spells.Terran

import Lifecycle.With
import Micro.Actions.Action
import Micro.Agency.Commander
import Micro.Actions.Combat.Spells.Spell
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

object WraithUncloak extends Action with Spell {

  def isCaster(unit: UnitInfo): Boolean = Terran.Wraith(unit)

  def canCast(unit: UnitInfo): Boolean = Terran.Wraith(unit) && unit.cloaked
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    unit.is(Terran.Wraith)
    && unit.cloaked
    && ! unit.matchups.enemies.exists(_.unitClass.attacksAir)
    && With.framesSince(unit.agent.lastCloak) > 24 * 10
  )
  
  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    Commander.decloak(unit, Terran.GhostCloak)
  }
}
