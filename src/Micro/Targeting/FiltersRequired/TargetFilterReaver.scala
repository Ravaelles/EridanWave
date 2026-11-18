package Micro.Targeting.FiltersRequired

import Micro.Targeting.TargetFilter
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

object TargetFilterReaver extends TargetFilter {
  override def appliesTo(actor: FriendlyUnitInfo): Boolean = Protoss.Reaver(actor)
  override def legal(actor: FriendlyUnitInfo, target: UnitInfo): Boolean = {

    if ( ! ReaverTargetRules.allowsBuilding(target.unitClass.isBuilding, target.unitClass.isCombatBuilding)) return false

    // Escort-first: a scarab belongs in the closing pack, never in the
    // static behind it. Only hittable mobiles count - an air threat the
    // hull cannot touch must not disarm it against the guns below.
    if (target.unitClass.isBuilding && ! ReaverTargetRules.yieldsToEscort(
      isBuilding = true,
      mobileThreatInRange = actor.matchups.threatsInRange.exists(t => ! t.unitClass.isBuilding && actor.canAttack(t)))) return false

    if (actor.agent.ride.isDefined) {
      val worthAttacking = (
        target.proxied
          || target.unitClass.attacksOrCastsOrDetectsOrTransports
          || target.base.exists(b => b.workerCount == 0 && b.resources.forall(u => u.tile.visible)))

      if ( ! worthAttacking) return false

      // Problems with the "don't get shot by a siege tank" filter
      // - risk is predicated on firing pixel which we don't have yet
      // - we can be shot by targets other than the tank
      // so it stays off until the firing pixel is known.

      return true
    }

    if (actor.inRangeToAttack(target))                      return true
    if (actor.matchups.threatNearest.contains(target))      return true
    if (actor.matchups.pixelsToThreatRange.forall(_ > 96))  return true

    false
  }
}
