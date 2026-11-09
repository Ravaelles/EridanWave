package Micro.Actions.Protoss

import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/** Land next to the fighting lead passenger instead of alone. */
object ReaverLandWithLead {

  def applies(situation: ReaverSituation): Boolean =
    situation.hasTransport &&
      situation.shouldFight &&
      (situation.scarabs > 0 || situation.traineeSoon) &&
      situation.walkableTile &&
      situation.toAttackPath &&
      situation.leadLanding &&
      situation.unloadSpotSafe

  def act(unit: FriendlyUnitInfo): Unit = {
    Commander.requestUnload(unit)
  }
}
