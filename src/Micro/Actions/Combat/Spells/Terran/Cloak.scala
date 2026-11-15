package Micro.Actions.Combat.Spells.Terran

import Micro.Actions.Action
import Micro.Actions.Combat.Spells.Spell
import Micro.Agency.Commander
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

object Cloak extends Action with Spell {

  // Cloak orders never report under their own name (like the four actions
  // they replace): the cast itself is the leaf, Cast is the container.
  override def isContainer: Boolean = true

  def isCaster(unit: UnitInfo): Boolean =
    CloakPolicy.hullFor(unit).isDefined

  def canCast(unit: UnitInfo): Boolean =
    CloakPolicy.hullFor(unit).exists(h =>
      unit.player.hasTech(h.tech) && (unit.cloaked || unit.energy >= h.tech.energyCost + h.energyReserve))

  override def allowed(unit: FriendlyUnitInfo): Boolean =
    CloakPolicy.hullFor(unit).exists(h => h.cloak(unit) || h.uncloak(unit))

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    CloakPolicy.hullFor(unit).foreach(h => {
      if (unit.cloaked) Commander.decloak(unit, h.tech)
      else Commander.cloak(unit, h.tech)
    })
  }
}
