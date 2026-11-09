package Micro.Actions.Protoss

import Micro.Agency.Commander
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
    unit.agent.act("HoldToFire")
    Commander.hold(unit)
  }
}
