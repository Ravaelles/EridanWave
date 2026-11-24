package Micro.Actions.Combat.Decisioning.Skirmish

import Mathematics.Maff
import Micro.Agency.Commander
import Micro.Actions.Combat.Decisioning.{Combat, HoldLineAdvance}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.ClusterPosture

/**
 * HoldLine advance behavior: a dressed ranged line that is holding walks
 * its formation at the enemy in fixed micro-steps instead of freezing
 * until the timeout or breaking into a charge. Fires only while the
 * cluster attacks (Fight posture), the weapon is cold and the target
 * sits past the gun - every other case already owns a behavior.
 * Returns true when it stepped.
 */
object HoldLineAdvanceBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    val target = combat.target
    val beyondTiles = target.map(t => unit.pixelsToGetInRange(t) * Maff.inv32).getOrElse(-1.0)
    val advancing =
      ! unit.squad.flatMap(_.battlePosture).exists(_ != ClusterPosture.Fight) &&
      ! combat.breakFormationToAttack &&
      target.nonEmpty &&
      HoldLineAdvance.shouldAdvance(
        hasFormation    = unit.formationEngage.isDefined,
        weaponReady     = unit.cooldownLeft <= 0,
        fractionInSlot  = combat.formationReadiness,
        beyondRangeTiles = beyondTiles)
    if ( ! advancing) {
      unit.agent.advanceAnchor = None

      return false
    }

    // The anchor walks the formation at the enemy instead of re-aiming
    // from wherever each hull drifted; cleared above the moment the
    // line stops advancing, so it never drags the unit back. The step
    // scales with the local odds: stronger lines walk bolder.
    val anchor = unit.agent.advanceAnchor.getOrElse(unit.pixel)
    val (usSkim, foeSkim) = unit.combatForecast
      .map(battle => (battle.us.skimStrengthTotal, battle.enemy.skimStrengthTotal))
      .getOrElse((0.0, 0.0))
    val stepped = anchor.project(target.get.pixel, HoldLineAdvance.stepTiles(beyondTiles, HoldLineAdvance.advanceTilesFor(usSkim, foeSkim)) * 32.0).walkablePixel
    unit.agent.advanceAnchor = Some(stepped)

    unit.agent.act("HoldAdvance")
    unit.agent.decision.set(stepped)
    Commander.move(unit)

    true
  }
}
