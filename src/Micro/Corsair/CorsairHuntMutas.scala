package Micro.Corsair

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.SquadAcePilots

/**
 * Healthy Corsairs answer Mutalisk packs: close ones first (most wounded,
 * then nearest), otherwise the pack nearest our main, otherwise nearest.
 * Weak Corsairs (42 total HP or less) never trade with Mutalisks.
 */
object CorsairHuntMutas {

  val MinHp = 42

  def mutaFor(unit: FriendlyUnitInfo): Option[UnitInfo] = {
    if (Protoss.Corsair(unit) && With.enemies.exists(_.isZerg) && unit.totalHealth > MinHp) {
      val mutas = CorsairTargets.liveEnemiesOf(Zerg.Mutalisk)
      val close = mutas.filter(_.pixelDistanceSquared(unit.pixel) <= CorsairTargets.CloseRadiusSq)
      if (close.nonEmpty) {
        CorsairTargets.closestWoundedFirst(close, unit.pixel)
      } else {
        Maff.minBy(mutas)(_.pixelDistanceSquared(With.geography.ourMain.heart.center))
          .orElse(Maff.minBy(mutas)(_.pixelDistanceSquared(unit.pixel)))
      }
    } else {
      None
    }
  }

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    mutaFor(unit).exists(muta =>
      CorsairAct.order(squad, unit, "CorsairHuntMutas", muta, muta.pixel))
  }
}
