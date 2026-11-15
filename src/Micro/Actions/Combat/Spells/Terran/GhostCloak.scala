package Micro.Actions.Combat.Spells.Terran

import Lifecycle.With
import Micro.Actions.Action
import Micro.Agency.Commander
import Micro.Actions.Combat.Spells.Spell
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

object GhostCloak extends Action with Spell {

  def isCaster(unit: UnitInfo): Boolean = Terran.Ghost(unit)

  def canCast(unit: UnitInfo): Boolean = (
    unit.player.hasTech(Terran.GhostCloak)
    && Terran.Ghost(unit)
    && ! unit.cloaked
    && unit.energy >= Terran.GhostCloak.energyCost + 30)
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = {
    With.self.hasTech(Terran.GhostCloak)                                    &&
    unit.is(Terran.Ghost)                                                   &&
    ! unit.cloaked                                                          &&
    unit.energy >= Terran.GhostCloak.energyCost + 30                        &&
    unit.matchups.framesOfSafety < 12 + With.reaction.agencyAverage &&
    ! With.grids.enemyDetection.inRange(unit.tile)
  }
  
  override protected def perform(unit: FriendlyUnitInfo) {
    Commander.cloak(unit, Terran.GhostCloak)
  }
  
}
