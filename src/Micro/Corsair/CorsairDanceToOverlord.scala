package Micro.Corsair

import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.support.SquadAcePilots

/**
 * Mid-duel Overlord dance: a cooling Corsair holds at standoff instead of
 * closing into the Spore math behind the Overlord; ours shoots the moment
 * it is ready. Close (or already firing) Corsairs go for the kill.
 */
object CorsairDanceToOverlord {

  val DanceCooldown = 9

  def shouldDance(cooldownLeft: Int, distTiles: Double, wound01: Double): Boolean =
    cooldownLeft >= DanceCooldown && distTiles >= 3.0 + wound01 * 2.0

  def duelOverlord(squad: SquadAcePilots, unit: FriendlyUnitInfo): Option[UnitInfo] = {
    val assigned = squad.targets.toSeq.flatten.filter(o => Zerg.Overlord(o))
    Maff.minBy(assigned)(_.pixelDistanceSquared(unit.pixel))
      .orElse(unit.matchups.targetNearest.filter(o => Zerg.Overlord(o)))
  }

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit)) return false

    duelOverlord(squad, unit).exists(overlord => {
      if (shouldDance(unit.cooldownLeft, unit.pixelDistanceCenter(overlord.pixel) / 32.0, unit.woundPercent())) {
        unit.intend(squad).setTargets(Seq(overlord)).setTerminus(unit.pixel)
      } else {
        unit.intend(squad).setTargets(Seq(overlord)).setTerminus(overlord.pixel)
      }
      unit.agent.act("CorsairDance")
      true
    })
  }
}
