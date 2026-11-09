package Micro.Actions.Protoss

import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/** Deliver the ordered shot, but only onto a safe spot. */
object ReaverUnloadToFight {

  def applies(situation: ReaverSituation): Boolean =
    situation.reaveOrdered &&
      situation.hasTransport &&
      situation.targetPath &&
      situation.unloadSpotSafe

  def act(unit: FriendlyUnitInfo): Unit = {
    Commander.requestUnload(unit)
  }
}
