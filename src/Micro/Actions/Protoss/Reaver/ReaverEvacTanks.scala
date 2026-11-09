package Micro.Actions.Protoss.Reaver

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/** Against Terran a landed Reaver trades its shot for a siege hit. */
object ReaverEvacTanks {

  def applies(situation: ReaverSituation): Boolean =
    ! situation.loaded &&
      ! situation.committed &&
      situation.isTerran &&
      situation.framesSinceFired < 3 * 24 &&
      (situation.shieldsDown >= 35 || situation.tankAiming)

  def act(unit: FriendlyUnitInfo): Unit = {
    unit.agent.act("HopTank")
    BeReaver.clickShuttle(unit)
  }
}
