package Micro.Corsair

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.SquadAcePilots

/**
 * Unmolested Corsairs hunt Overlords instead of waiting: close ones first
 * (most wounded, then nearest), otherwise the nearest known one on the
 * board. A Corsair under fire keeps flying - it does not chase.
 */
object CorsairHunt {

  def huntTarget(unit: FriendlyUnitInfo): Option[UnitInfo] = {
    if ( ! With.enemies.exists(_.isZerg)) return None

    val overlords = With.units.enemy.filter(o => Zerg.Overlord(o) && o.likelyStillThere).toSeq
    val close = overlords.filter(_.pixelDistanceSquared(unit.pixel) <= 6 * 6 * 32 * 32)
    if (close.nonEmpty) {
      Maff.minBy(close)(o => (-o.woundPercent(), o.pixelDistanceSquared(unit.pixel)))
    } else {
      Maff.minBy(overlords)(_.pixelDistanceSquared(unit.pixel))
    }
  }

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit) || With.framesSince(unit.lastFrameTakingDamage) <= 3 * 24) return false

    huntTarget(unit).exists(overlord => {
      if (unit.cooldownLeft >= 10 && unit.pixelDistanceCenter(overlord.pixel) >= 5 * 32) {
        unit.intend(squad).setTerminus(overlord.pixel)
      } else {
        unit.intend(squad).setTargets(Seq(overlord)).setTerminus(overlord.pixel)
      }
      unit.agent.act("CorsairHunt")
      true
    })
  }
}
