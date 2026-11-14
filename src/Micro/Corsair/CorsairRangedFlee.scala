package Micro.Corsair

import Lifecycle.With
import Micro.Actions.Combat.Maneuvering.{RangedAvoidance, RangedReach}
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.support.SquadAcePilots

/**
 * Ranged-threat flee (first claim in the brain): a Corsair inside the
 * reach of any enemy ranged unit breaks off instead of trading hull for
 * nothing - a dying hull turns a full 10 tiles out, a fresh one skates
 * at 3. Survival outranks hunting, relocating and exploring.
 */
object CorsairRangedFlee {

  /** Shared avoidance: any enemy ranged unit, wound-scaled 3-to-10 tile margin. */
  val avoidance = new RangedAvoidance(marginLowTiles = 3.0, marginHighTiles = 10.0)

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit)) return false

    val threats = With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && e.unitClass.canAttack(unit)
        && RangedReach.isRanged(e.pixelRangeAgainst(unit))).map(_.pixel).toSeq
    avoidance.fleePixel(unit.pixel, threats, unit.woundPercent()).exists(dest => {
      unit.intend(squad).setTerminus(dest)
      unit.agent.act("CorsairFleeRanged")
      true
    })
  }
}
