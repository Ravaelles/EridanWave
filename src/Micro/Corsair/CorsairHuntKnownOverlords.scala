package Micro.Corsair

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.SquadAcePilots

/**
 * Visit remembered Overlords across the whole known board (not just the
 * neighbourhood): the nearest to our main that no anti-air weapon covers.
 * Covered Overlords are left alone - visiting means dying.
 */
object CorsairHuntKnownOverlords {

  val AaCoverTiles = 7.1

  def overlord(): Option[UnitInfo] = {
    if ( ! With.enemies.exists(_.isZerg)) {
      None
    } else {
      With.units.enemy.filter(o => Zerg.Overlord(o) && o.likelyStillThere).toSeq
        .sortBy(_.pixelDistanceSquared(With.geography.ourMain.heart.center))
        .find(o => ! covered(o))
    }
  }

  private def covered(overlord: UnitInfo): Boolean =
    With.units.enemy.exists(e => e.likelyStillThere && e.canAttackAir
      && e.pixelDistanceCenter(overlord.pixel) < AaCoverTiles * 32)

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit)) {
      false
    } else {
      overlord().exists(target =>
        CorsairAct.order(squad, unit, "CorsairHuntKnown", target, target.pixel))
    }
  }
}
