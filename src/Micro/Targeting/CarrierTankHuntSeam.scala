package Micro.Targeting

import Lifecycle.With
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.UnitFilters.IsTank

/**
 * Game-coupled seam for carrier doctrines (reads the world, decides
 * nothing): tank-hunt state plus the wing's combined live interceptors
 * for the brawl rule. Thin on purpose; the doctrines stay pure and tested.
 */
object CarrierTankHuntSeam {

  def wingInterceptors: Int =
    With.units.ours.view.filter(Protoss.Carrier).flatMap(_.interceptors).count(_.complete)

  def brawlingFor(attacker: FriendlyUnitInfo): Boolean =
    CarrierBrawl.shouldBrawl(wingInterceptors)

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
