package Micro.Actions.Protoss

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/** A drop resets the cooldown to 30: hop instead of waiting it out. */
object ReaverRefreshCooldown {

  def applies(situation: ReaverSituation): Boolean =
    ! situation.loaded && situation.cooling

  def act(unit: FriendlyUnitInfo): Unit = {
    unit.agent.act("Refresh")
    BeReaver.clickShuttle(unit)
  }
}
