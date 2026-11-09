package Micro.Corsair

import Lifecycle.With
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.SquadAcePilots

/**
 * Anti-idle relocation: a Corsair that fired nothing for a while goes
 * elsewhere instead of holding its pose. The destination is deterministic
 * per (unit, 6-second bucket): smooth within the bucket, re-rolled across
 * buckets, with no cross-game state to reset.
 */
object CorsairRelocateWhenIdle {

  def applies(unit: FriendlyUnitInfo): Boolean = {
    val airNear = With.units.enemy.filter(e =>
      e.flying && e.likelyStillThere && e.pixelDistanceSquared(unit.pixel) <= 14 * 14 * 32 * 32).toSeq
    if (airNear.nonEmpty) {
      false
    } else if (unit.totalHealth <= 95 && unit.lastUnderAttackLessThanAgo(6 * 24)) {
      true
    } else if (With.units.enemy.exists(e =>
      ! e.unitClass.isBuilding && unit.canAttack(e) && e.pixelDistanceSquared(unit.pixel) <= 14 * 14 * 32 * 32)) {
      false
    } else if (With.framesSince(unit.lastFrameStartingAttack) <= 7 * 24) {
      false
    } else {
      With.units.enemy.exists(_.pixelDistanceSquared(unit.pixel) <= 14 * 14 * 32 * 32)
    }
  }

  def relocateOffset(id: Int, bucket: Int, delta: Int): (Int, Int) = {
    val span = 2 * delta + 1
    val dx = -delta + ((id * 31 + bucket * 17) % span + span) % span
    val dy = -delta + ((id * 57 + bucket * 29) % span + span) % span
    (dx, dy)
  }

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit) || ! applies(unit)) {
      false
    } else {
      val delta = 12 + unit.id % 7
      val offset = relocateOffset(unit.id, With.frame / (24 * 6), delta)
      val dest = unit.pixel.add(offset._1 * 32, offset._2 * 32).clamp()
      unit.intend(squad).setTerminus(dest)
      unit.agent.act("CorsairRelocate")
      true
    }
  }
}
