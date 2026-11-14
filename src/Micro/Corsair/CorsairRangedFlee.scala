package Micro.Corsair

import Lifecycle.With
import Micro.Actions.Combat.Maneuvering.AsAirAvoidLandUnits
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.support.SquadAcePilots

/**
 * Ranged-threat flee (first claim in the brain): a Corsair inside the
 * reach of any enemy unit that can hit it breaks off instead of trading
 * hull for nothing - a dying hull turns a full ten tiles out, a fresh
 * one skates at three. Survival outranks hunting, relocating and
 * exploring. Runs on the shared AsAirAvoidLandUnits rule (centroid of
 * everything that can hit us, corner-guarded) so air doctrines cannot
 * drift apart on where safety ends.
 */
object CorsairRangedFlee {

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit)) return false

    val threats = With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && e.unitClass.canAttack(unit)).map(_.pixel).toSeq
    AsAirAvoidLandUnits.fleePixel(unit.pixel, threats, unit.woundPercent(), With.mapPixelWidth, With.mapPixelHeight).exists(dest => {
      unit.intend(squad).setTerminus(dest)
      unit.agent.act("CorsairFleeRanged")
      true
    })
  }
}
