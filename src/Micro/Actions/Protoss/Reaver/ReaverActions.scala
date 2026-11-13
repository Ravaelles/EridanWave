package Micro.Actions.Protoss.Reaver

import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Shared Reaver orders both disciplines use: boarding the ride is one
 * right-click, and it must not live on either discipline, or the legacy
 * import points back at the dispatcher and the two form a cycle.
 */
object ReaverActions {

  /** The engine resets a dropped Reaver's cooldown to this many frames. */
  val cooldownOnLanding: Int = 30

  /** Stand ground to spend a shot: one order, a distinct trace label. */
  def holdGround(unit: FriendlyUnitInfo, label: String): Unit = {
    unit.agent.act(label)
    Commander.hold(unit)
  }

  def clickShuttle(unit: FriendlyUnitInfo): Unit = {
    Commander.rightClick(unit, unit.agent.ride.get)
  }
}
