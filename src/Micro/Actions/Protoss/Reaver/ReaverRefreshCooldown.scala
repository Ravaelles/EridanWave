package Micro.Actions.Protoss.Reaver

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/** A drop resets the cooldown to 30: hop instead of waiting it out, but only under pressure. */
object ReaverRefreshCooldown {

  def applies(situation: ReaverSituation): Boolean =
    ! situation.loaded && situation.cooling && situation.pressured

  def act(unit: FriendlyUnitInfo): Unit = {
    unit.agent.act("Refresh")
    ReaverActions.clickShuttle(unit)
  }
}
