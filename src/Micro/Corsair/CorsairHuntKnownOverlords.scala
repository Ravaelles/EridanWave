package Micro.Corsair

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.support.SquadAcePilots

/**
 * Visit remembered Overlords near the Corsair (not just the neighbourhood,
 * but never across the map): the nearest to the unit that no anti-air
 * weapon covers. Covered Overlords are left alone - visiting means dying.
 * Per-unit nearest keeps the squad distributed instead of converging every
 * Corsair onto one Overlord by our main (override contract: never widen).
 */
object CorsairHuntKnownOverlords {

  val AaCoverTiles = 7.1

  def overlord(unit: FriendlyUnitInfo): Option[UnitInfo] = {
    if ( ! With.enemies.exists(_.isZerg)) return None

    With.units.enemy.filter(o => Zerg.Overlord(o) && o.likelyStillThere).toSeq
      .sortBy(_.pixelDistanceSquared(unit.pixel))
      .find(o => ! (o.hitPoints <= 0 && o.visible) && ! covered(o))
  }

  private def covered(overlord: UnitInfo): Boolean =
    With.units.enemy.exists(e => e.likelyStillThere && e.canAttackAir
      && e.pixelDistanceCenter(overlord.pixel) < AaCoverTiles * 32)

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit)) return false

    overlord(unit).exists(target => {
      unit.intend(squad).setTargets(Seq(target)).setTerminus(target.pixel)
      unit.agent.act("CorsairHuntKnown")
      true
    })
  }
}
