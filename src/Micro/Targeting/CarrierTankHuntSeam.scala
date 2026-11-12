package Micro.Targeting

import Lifecycle.With
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.UnitFilters.IsTank

/**
 * Game-coupled seam for CarrierTankHunt (reads the world, decides nothing):
 * a tank of ours in range, our combined live interceptors, enemy AA near
 * the attacker. Thin on purpose; the doctrine itself stays pure and tested.
 */
object CarrierTankHuntSeam {

  def huntFor(attacker: FriendlyUnitInfo): Boolean = {
    val tankNear = attacker.matchups.targets.exists(t =>
      IsTank(t) && attacker.tileDistanceEdge(t) <= CarrierTankHunt.tankHuntTiles)
    if ( ! tankNear) return false

    val totalInterceptors = With.units.ours.view
      .filter(Protoss.Carrier)
      .flatMap(_.interceptors)
      .count(_.complete)
    val enemyAANear = attacker.matchups.enemies.count(e =>
      e.unitClass.attacksAir && attacker.pixelDistanceEdge(e) <= 32 * CarrierTankHunt.tankHuntTiles)
    CarrierTankHunt.shouldHunt(tankNear, totalInterceptors, enemyAANear)
  }
}
