package Micro.Actions.Protoss.Reaver

import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/** Deliver the ordered shot, but only onto a safe spot - unless the pair is healthy enough to brave it. */
object ReaverUnloadToFight {

  def applies(situation: ReaverSituation): Boolean =
    situation.reaveOrdered &&
      situation.hasTransport &&
      situation.targetPath &&
      (situation.unloadSpotSafe || situation.braveDrop)

  def act(unit: FriendlyUnitInfo): Unit = {
    Commander.requestUnload(unit)
  }
}
