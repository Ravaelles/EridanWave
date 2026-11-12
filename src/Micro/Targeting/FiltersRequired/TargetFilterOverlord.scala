package Micro.Targeting.FiltersRequired

import Micro.Targeting.{OverlordSnipe, TargetFilter}
import ProxyBwapi.Races.Zerg
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Required Overlord filter: everything else shoots first. An Overlord is
 * only a legal target inside our firing range while no other enemy we can
 * hit is in range too - combat units and workers alike outrank it, so the
 * army never chases a fleeing Overlord out of a fight it was winning.
 * Corsairs keep their hunting: what they cannot hit does not count as
 * an alternative.
 */
object TargetFilterOverlord extends TargetFilter {

  override def legal(attacker: FriendlyUnitInfo, target: UnitInfo): Boolean = {
    if ( ! Zerg.Overlord(target)) return true

    OverlordSnipe.allowed(
      attacker.inRangeToAttack(target),
      attacker.matchups.targetsInRange.count(t => t.id != target.id && attacker.canAttack(t)))
  }
}
