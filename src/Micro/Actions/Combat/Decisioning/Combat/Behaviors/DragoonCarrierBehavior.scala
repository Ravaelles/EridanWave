package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Protoss.Dragoon.DragoonCarrierHunt
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Dragoon carrier behavior: a Dragoon that would otherwise retreat shoots
 * a Carrier in range while its shields hold twenty, and closes a pathed
 * Carrier inside eleven tiles - to the hull with a ground path, else to
 * the halfway point with one. Steals only the retreat verdict, never the
 * fight - engaging guns keep their picks. Runs below the siege keep-off
 * and beside the Terran stand.
 */
object DragoonCarrierBehavior {

  def run(unit: FriendlyUnitInfo, situation: CombatSituation): Boolean = {
    if ( ! Protoss.Dragoon(unit)) return false
    if ( ! situation.shouldRetreat) return false
    // Dying hulls keep their retreat: seventy hit points buy no Carrier
    // trade, only a donation - the interceptors do not miss.
    if ( ! DragoonCarrierHunt.healthyEnough(unit.totalHealth)) return false
    if (unit.shieldPoints < DragoonCarrierHunt.shieldFloor) return false

    val carrier = Maff.minBy(unit.matchups.enemies.view.filter(e =>
      e.alive && Protoss.Carrier(e)))(
      unit.pixelDistanceEdge(_))
    if (carrier.isEmpty) return false

    if (unit.inRangeToAttack(carrier.get)) {
      unit.agent.act("DragoonCarrier")
      Commander.attack(unit, carrier.get)

      return true
    }

    val carrierTiles = unit.pixelDistanceEdge(carrier.get) * Maff.inv32
    if ( ! DragoonCarrierHunt.shouldClose(isDragoon = true, unit.shieldPoints, carrierTiles, pathed = true)) return false

    val goal =
      if (With.paths.groundPathExists(unit.tile, carrier.get.tile)) carrier.get.pixel
      else {
        val halfway = unit.pixel.midpoint(carrier.get.pixel)
        if (With.paths.groundPathExists(unit.tile, halfway.tile)) halfway else return false
      }
    unit.agent.act("DragoonCarrier")
    unit.agent.forceTo(goal)
    Commander.move(unit)

    true
  }
}
