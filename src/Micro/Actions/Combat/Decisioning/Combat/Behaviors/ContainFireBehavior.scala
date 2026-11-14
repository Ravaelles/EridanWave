package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.ContainStatus

/**
 * Contain fire: while the squad contains, a ready weapon shoots the
 * nearest hittable target - buildings included - instead of dressing,
 * kiting or repositioning past it. Outranks everything below the
 * crucial-air snipe. Returns true when it shot or already holds a shot.
 */
object ContainFireBehavior {

  def run(unit: FriendlyUnitInfo): Boolean = {
    if ( ! unit.agent.shouldFight) return false

    if ( ! ContainStatus.now()) return false

    val hittable = unit.matchups.targetsInRange
    if (hittable.isEmpty) return false

    if ( ! ContainFire.shouldShoot(cooldownLeft = unit.cooldownLeft, hasHittableTarget = hittable.nonEmpty)) return false

    val target = Maff.minBy(hittable)(unit.pixelDistanceCenter(_)).get
    if ( ! unit.orderTarget.contains(target)) {
      unit.agent.act("ContainFire")
      Commander.attack(unit, target)
    }

    true
  }
}
