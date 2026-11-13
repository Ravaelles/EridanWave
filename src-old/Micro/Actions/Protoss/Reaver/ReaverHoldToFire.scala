package Micro.Actions.Protoss.Reaver

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/** Hold a firing position instead of walking into the enemy. */
object ReaverHoldToFire {

  def applies(situation: ReaverSituation): Boolean =
    ! situation.loaded &&
      ! situation.committed &&
      situation.scarabs > 0 &&
      ! situation.hasActiveScarab &&
      situation.holdTarget

  def act(unit: FriendlyUnitInfo): Unit = {
    ReaverActions.holdGround(unit, "HoldToFire")
  }
}
