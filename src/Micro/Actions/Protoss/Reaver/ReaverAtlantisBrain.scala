package Micro.Actions.Protoss.Reaver

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Ordered Reaver discipline: the first behavior whose situation applies
 * acts, the rest stand down. Order is the contract - rescue a flying
 * Scarab, evacuate tanks, board under pressure, deliver the shot, reset
 * the cooldown, hold to fire. A new behavior slots in as one object plus
 * one line here.
 */
object ReaverAtlantisBrain {

  def act(unit: FriendlyUnitInfo): Unit = {
    val situation = ReaverSituation.read(unit)
    if ( ! situation.rideNearby) return

    if (ReaverRescueScarab.applies(situation)) {
      ReaverRescueScarab.act(unit)
    } else if (ReaverEvacTanks.applies(situation)) {
      ReaverEvacTanks.act(unit)
    } else if (ReaverBoardPressure.applies(situation)) {
      ReaverBoardPressure.act(unit)
    } else if (ReaverUnloadToFight.applies(situation)) {
      ReaverUnloadToFight.act(unit)
    } else if (ReaverLandWithLead.applies(situation)) {
      ReaverLandWithLead.act(unit)
    } else if (ReaverRefreshCooldown.applies(situation)) {
      ReaverRefreshCooldown.act(unit)
    } else if (ReaverHoldToFire.applies(situation)) {
      ReaverHoldToFire.act(unit)
    }
  }
}
