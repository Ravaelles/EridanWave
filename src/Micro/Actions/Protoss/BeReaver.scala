package Micro.Actions.Protoss

import Lifecycle.With
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Reaver transport discipline, Strategy pattern: the legacy PurpleWave
 * behavior stays intact as BeReaverLegacy; the default is the Atlantis
 * chain (BeReaverAtlantis). Flip With.blackboard.reaverAtlantisBehavior
 * to compare or fall back.
 */
object BeReaver extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.Reaver(unit)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    if (With.blackboard.reaverAtlantisBehavior()) BeReaverAtlantis(unit) else BeReaverLegacy(unit)
  }

  def clickShuttle(unit: FriendlyUnitInfo): Unit = {
    Commander.rightClick(unit, unit.agent.ride.get)
  }
}
