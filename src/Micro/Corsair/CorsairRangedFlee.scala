package Micro.Corsair

import Lifecycle.With
import Micro.Actions.Combat.Maneuvering.{AirFlee, CornerAntiStuckGuard, FleeRelocation}
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.support.SquadAcePilots

/**
 * Ranged-threat flee (first claim in the brain): a Corsair inside the
 * reach of any enemy unit that can hit it breaks off instead of trading
 * hull for nothing - a dying hull turns a full ten tiles out, a fresh
 * one skates at three. Survival outranks hunting, relocating and
 * exploring. Runs on the shared AirFlee rule (centroid of
 * everything that can hit us, corner-guarded) so air doctrines cannot
 * drift apart on where safety ends.
 */
object CorsairRangedFlee {

  /**
   * Corsair trigger radius, in tiles: Corsairs start their run two tiles
   * outside the shared air rule - ground guns outrange the skirmish, so
   * reacting at twelve means reacting inside their reach.
   */
  val corsairTriggerTiles: Double = 14.0

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit)) return false

    val threats = With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && e.unitClass.canAttack(unit)).map(_.pixel).toSeq
    AirFlee.fleePixel(
      unit.pixel,
      threats,
      unit.woundPercent(),
      With.mapPixelWidth,
      With.mapPixelHeight,
      AirFlee.marginLowTiles,
      AirFlee.marginHighTiles,
      corsairTriggerTiles,
      CornerAntiStuckGuard.corsairTiles).exists(away => {
      // Aired out: fleeing the same line without getting anywhere means
      // cornered or herded, so rotate the escape instead of milling.
      val dest = CornerAntiStuckGuard.guard(
        FleeRelocation.relocate(unit.id, unit.pixel, away, With.frame),
        With.mapPixelWidth,
        With.mapPixelHeight,
        CornerAntiStuckGuard.corsairTiles)
      unit.intend(squad).setTerminus(dest)
      unit.agent.act("CorsairFleeRanged")
      true
    })
  }
}
