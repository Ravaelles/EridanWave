package Micro.Actions.Protoss

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/** A flying Scarab cannot be spent twice: board when pressured mid-shot. */
object ReaverRescueScarab {

  def applies(situation: ReaverSituation): Boolean =
    ! situation.loaded &&
      ! situation.committed &&
      situation.hasActiveScarab &&
      situation.cooldownLeft <= situation.latencyFrames &&
      situation.threatened

  def act(unit: FriendlyUnitInfo): Unit = {
    unit.agent.act("Hop")
    BeReaver.clickShuttle(unit)
  }
}
