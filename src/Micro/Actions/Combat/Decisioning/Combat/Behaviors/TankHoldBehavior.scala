package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Tank hold (thin seam over its kernel): a ranged ground warrior whose
 * duel points at a tank holds the line instead of closing it - walking
 * in trades unanswered shells for nothing, while standing still shoots
 * whatever walks into our own reach. Runs after the hug and the kite
 * (both keep their duels) and before the approach and the charge, so
 * the only way onto a tank is TankHug's point-blank exception. Retreating
 * hulls keep the retreat; Reavers keep their pair doctrine.
 */
object TankHoldBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if (unit.flying || unit.unitClass.melee || ! unit.unitClass.isWarrior || Protoss.Reaver(unit)) return false
    if (combat.shouldRetreat) return false

    val target = combat.target
    if (target.isEmpty || ! TankHold.isTank(target.get)) return false
    // Beyond our own max range, in tiles: pixels past the gun, not the
    // travelling kind - the hold is about the shot, not the path.
    val beyondTiles =
      (unit.pixelDistanceEdge(target.get) - unit.pixelRangeGround) * Maff.inv32
    if ( ! TankHold.shouldHold(targetIsTank = true, distBeyondRangeTiles = beyondTiles)) return false

    unit.agent.act("TankHold")
    Commander.hold(unit)

    true
  }
}
