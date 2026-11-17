package Micro.Actions.Combat.Decisioning.Skirmish

import Mathematics.Maff
import Micro.Agency.Commander
import Micro.Actions.Combat.Decisioning.{Combat, HoldLineCreep}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.ClusterPosture

/**
 * HoldLine creep behavior: a dressed ranged line that is holding walks
 * its formation at the enemy in fixed micro-steps instead of freezing
 * until the timeout or breaking into a charge. Fires only while the
 * cluster attacks (Fight posture), the weapon is cold and the target
 * sits past the gun - every other case already owns a behavior.
 * Returns true when it stepped.
 */
object HoldLineCreepBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    val target = combat.target
    val beyondTiles = target.map(t => unit.pixelsToGetInRange(t) * Maff.inv32).getOrElse(-1.0)
    val creeping =
      ! unit.squad.flatMap(_.battlePosture).exists(_ != ClusterPosture.Fight) &&
      ! combat.breakFormationToAttack &&
      target.nonEmpty &&
      HoldLineCreep.shouldCreep(
        hasFormation    = unit.formationEngage.isDefined,
        melee           = unit.unitClass.melee,
        weaponReady     = unit.cooldownLeft <= 0,
        fractionInSlot  = combat.formationReadiness,
        beyondRangeTiles = beyondTiles)
    if ( ! creeping) {
      unit.agent.creepAnchor = None

      return false
    }

    // The anchor walks the formation at the enemy instead of re-aiming
    // from wherever each hull drifted; cleared above the moment the
    // line stops creeping, so it never drags the unit back.
    val anchor = unit.agent.creepAnchor.getOrElse(unit.pixel)
    val stepped = anchor.project(target.get.pixel, HoldLineCreep.stepTiles(beyondTiles) * 32.0).walkablePixel
    unit.agent.creepAnchor = Some(stepped)

    unit.agent.act("HoldCreep")
    unit.agent.decision.set(stepped)
    Commander.move(unit)

    true
  }
}
