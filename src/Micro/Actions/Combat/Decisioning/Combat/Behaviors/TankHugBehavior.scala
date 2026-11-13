package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import Micro.Agency.Commander
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Siege-hug behavior: a cooling ground warrior walks onto the nearest
 * sieged tank instead of kiting - at touching distance the tank cannot
 * answer, and the cooldown runs down either way. Runs before the kite
 * chain (hugging replaces yielding), after the push/assemble/scavenge
 * owners. Returns true when it moved or shot the tank.
 */
object TankHugBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! unit.agent.shouldFight) return false

    if (unit.flying) return false

    if ( ! unit.unitClass.isWarrior) return false

    if ( ! unit.canMove) return false

    val tank = Maff.minBy(unit.matchups.enemies.view.filter(e =>
      e.alive && Terran.SiegeTankSieged(e) && unit.tileDistanceEdge(e) <= TankHug.siegeRadiusTiles))(
      unit.pixelDistanceEdge(_))
    if (tank.isEmpty) return false

    if ( ! TankHug.shouldHug(
      siegedTankTiles = unit.tileDistanceEdge(tank.get),
      cooldownLeft    = unit.cooldownLeft,
      shouldFight     = unit.agent.shouldFight,
      canMove         = unit.canMove)) return false

    unit.agent.act("HugTank")
    if (unit.tileDistanceEdge(tank.get) > TankHug.hugGapTiles) {
      Commander.move(unit, tank.get.pixel)
    } else {
      Commander.attack(unit, tank.get)
    }

    true
  }
}
