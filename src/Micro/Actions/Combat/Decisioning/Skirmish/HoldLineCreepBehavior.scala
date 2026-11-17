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
    if (unit.squad.flatMap(_.battlePosture).exists(_ != ClusterPosture.Fight)) return false
    if (combat.breakFormationToAttack) return false

    val target = combat.target
    if (target.isEmpty) return false

    val beyondTiles = unit.pixelsToGetInRange(target.get) * Maff.inv32
    if ( ! HoldLineCreep.shouldCreep(
      hasFormation    = unit.formationEngage.isDefined,
      melee           = unit.unitClass.melee,
      weaponReady     = unit.cooldownLeft <= 0,
      fractionInSlot  = combat.formationReadiness,
      beyondRangeTiles = beyondTiles)) return false

    unit.agent.act("HoldCreep")
    unit.agent.decision.set(unit.pixel.project(target.get.pixel, HoldLineCreep.stepTiles(beyondTiles) * 32.0).walkablePixel)
    Commander.move(unit)

    true
  }
}
