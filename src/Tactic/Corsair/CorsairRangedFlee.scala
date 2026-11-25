package Tactic.Corsair

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Maneuvering.{AirFlee, CornerAntiStuckGuard, FleeRelocation}
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.support.SquadAcePilots

/**
 * Ranged-threat flee (first claim in the brain): a Corsair inside the
 * reach of any enemy unit that can hit it breaks off instead of trading
 * hull for nothing - a dying hull turns a full ten tiles out, a fresh
 * one skates at three. Survival outranks hunting, relocating and
 * exploring. Runs on the shared scored AirFlee rule (plain-distance
 * penetration, sharp edge, worst threat sets the focus) so air doctrines
 * cannot drift apart on where safety ends.
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
    val marginPixels = AirFlee.marginTiles(unit.woundPercent(), AirFlee.marginLowTiles, AirFlee.marginHighTiles) * 32.0
    // Cornered corsairs flee the guns and the corner together: inside
    // the corner zone the escape blends the enemy centroid with the
    // corner push (guns weighted double), so the leg bends off the trap
    // instead of hugging it while the guns close in. Outside the zone
    // the scored worst-threat rule stands untouched.
    val inTrigger = threats.filter(t => unit.pixelDistanceCenter(t) < corsairTriggerTiles * 32.0)
    val corner = CornerAntiStuckGuard.nearestCorner(unit.pixel, With.mapPixelWidth, With.mapPixelHeight)
    val away =
      if (inTrigger.nonEmpty && CornerAntiStuckGuard.inCornerZone(unit.pixel, With.mapPixelWidth, With.mapPixelHeight, CornerAntiStuckGuard.corsairTiles))
        Some(CornerAntiStuckGuard.blendedEscape(unit.pixel, Maff.centroid(inTrigger), corner, marginPixels))
      else
        AirFlee.fleeScored(
          unit.pixel,
          threats.map(t => (t, corsairTriggerTiles * 32.0 - unit.pixelDistanceCenter(t))),
          _ > 0.0,
          marginPixels,
          With.mapPixelWidth,
          With.mapPixelHeight,
          CornerAntiStuckGuard.corsairTiles)
    away.exists(away => {
      // Aired out: fleeing the same line without getting anywhere means
      // cornered or herded, so rotate the escape instead of milling.
      val dest = CornerAntiStuckGuard.guard(
        FleeRelocation.relocate(unit.id, unit.pixel, away, With.frame),
        With.mapPixelWidth,
        With.mapPixelHeight,
        CornerAntiStuckGuard.corsairTiles)
      unit.squadIntend(squad).setTerminus(dest)
      unit.agent.act("CorsairFleeRanged")
      true
    })
  }
}
