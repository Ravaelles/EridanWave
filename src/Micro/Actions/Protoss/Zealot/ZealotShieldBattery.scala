package Micro.Actions.Protoss.Zealot

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?

/**
 * The battery a rushing Zealot can top up at. Zealot shields are free
 * health for a unit that trades on health, so a stop at a charged battery
 * is worth more than the seconds it costs.
 */
object ZealotShieldBattery {

  /**
   * Nearest battery holding at least twice the shields we are missing, and
   * only worth the walk when we are nearly dry unless it sits on our path.
   */
  def best(zealot: FriendlyUnitInfo): Option[UnitInfo] = {
    val charged = With.units.ours
      .filter(b =>
        Protoss.ShieldBattery(b)
        && b.complete
        && b.energy * 2 > zealot.unitClass.maxShields - zealot.shieldPoints
        && (zealot.shieldPoints < ?(zealot.pixelDistanceEdge(b) < 64, zealot.unitClass.maxShields, 10)))
      .toVector
    Maff.minBy(charged)(_.pixelDistanceEdge(zealot))
  }
}
