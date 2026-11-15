package Micro.Actions.Combat.Spells.Terran

import Lifecycle.With
import Micro.Actions.Action
import Micro.Agency.Commander
import Micro.Actions.Combat.Spells.Spell
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

object WraithCloak extends Action with Spell {

  def isCaster(unit: UnitInfo): Boolean = Terran.Wraith(unit)

  def canCast(unit: UnitInfo): Boolean = (
    unit.player.hasTech(Terran.WraithCloak)
    && Terran.Wraith(unit)
    && ! unit.cloaked
    && unit.energy >= Terran.WraithCloak.energyCost + 20)
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    Terran.WraithCloak()
    && Terran.Wraith(unit)
    && ! unit.cloaked
    && unit.confidence11 < 0.9
    && unit.matchups.pixelsToThreatRange.exists(_ < 32)
    && unit.energy >= Terran.WraithCloak.energyCost + 20
    && ! With.grids.enemyDetection.inRange(unit.tile))

  protected def perform(unit: FriendlyUnitInfo): Unit = {
    Commander.cloak(unit, Terran.WraithCloak)
  }
}
