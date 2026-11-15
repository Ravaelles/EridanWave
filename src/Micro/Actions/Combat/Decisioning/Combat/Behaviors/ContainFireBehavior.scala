package Micro.Actions.Combat.Decisioning

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

    NearestShot.take(unit, "ContainFire", hittable, unit.pixelDistanceCenter(_))
  }
}
