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
    if ( ! With.enemies.exists(_.isZerg)) {
      None
    } else {
      val overlords = CorsairTargets.liveEnemiesOf(Zerg.Overlord)
      val close = overlords.filter(_.pixelDistanceSquared(unit.pixel) <= CorsairTargets.CloseRadiusSq)
      if (close.nonEmpty) {
        CorsairTargets.closestWoundedFirst(close, unit.pixel)
      } else {
        Maff.minBy(overlords)(_.pixelDistanceSquared(unit.pixel))
      }
    }
  }

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit) || ! unit.lastUnderAttackMoreThanAgo(3 * 24)) {
      false
    } else {
      huntTarget(unit).exists(overlord => {
        // Terminus-only while closing: squad-assigned targets persist on the
        // intent, so the duel continues without narrowing focus mid-flight.
        if (unit.cooldownLeft >= 10 && unit.pixelDistanceCenter(overlord.pixel) >= 5 * 32) {
          CorsairAct.move(squad, unit, "CorsairHunt", overlord.pixel)
        } else {
          CorsairAct.order(squad, unit, "CorsairHunt", overlord, overlord.pixel)
        }
      })
    }
  }
}
