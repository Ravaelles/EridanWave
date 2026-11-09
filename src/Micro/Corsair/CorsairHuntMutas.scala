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
      val mutas = With.units.enemy.filter(m => Zerg.Mutalisk(m) && m.likelyStillThere).toSeq
      val close = mutas.filter(_.pixelDistanceSquared(unit.pixel) <= 6 * 6 * 32 * 32)
      if (close.nonEmpty) {
        Maff.minBy(close)(m => (-m.woundPercent(), m.pixelDistanceSquared(unit.pixel)))
      } else {
        Maff.minBy(mutas)(_.pixelDistanceSquared(With.geography.ourMain.heart.center))
          .orElse(Maff.minBy(mutas)(_.pixelDistanceSquared(unit.pixel)))
      }
    } else {
      None
    }
  }

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    mutaFor(unit).exists(muta => {
      unit.intend(squad).setTargets(Seq(muta)).setTerminus(muta.pixel)
      unit.agent.act("CorsairHuntMutas")
      true
    })
  }
}
