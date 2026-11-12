package Micro.Actions.Protoss.Reaver

import Lifecycle.With
import Micro.Actions.Action
import Micro.Actions.Protoss.ReaverLegacy.BeReaverLegacy
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
}
