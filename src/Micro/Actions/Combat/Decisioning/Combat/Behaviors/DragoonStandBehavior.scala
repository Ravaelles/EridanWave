package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Dragoon stand behavior: a Dragoon that would otherwise retreat shoots
 * the nearest Terran in range while its shields are fresh and its gun
 * is hot, instead of donating the shot it is about to have. Steals only
 * the retreat verdict, never the fight - engaging guns keep their picks.
 * Runs below the siege keep-off (shells still outrank a hot gun) and
 * above the retreat plans.
 */
object DragoonStandBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! Protoss.Dragoon(unit)) return false
    if ( ! combat.shouldRetreat) return false
    if (unit.shieldWound > DragoonTerranStand.maxShieldWound) return false
    if (unit.cooldownLeft > DragoonTerranStand.readyCooldownFrames) return false

    val prey = Maff.minBy(unit.matchups.targetsInRange.view.filter(e =>
      e.alive && e.unitClass.isTerran))(
      unit.pixelDistanceEdge(_))
    if (prey.isEmpty) return false
    if ( ! DragoonTerranStand.shouldStand(unit.shieldWound, unit.cooldownLeft, terranInRange = true)) return false

    unit.agent.act("DragoonStand")
    Commander.attack(unit, prey.get)
    true
  }
}
