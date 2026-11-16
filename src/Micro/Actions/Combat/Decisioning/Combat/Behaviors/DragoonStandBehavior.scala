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

  def run(unit: FriendlyUnitInfo, situation: CombatSituation): Boolean = {
    if ( ! Protoss.Dragoon(unit)) return false
    if ( ! situation.shouldRetreat) return false

    // Warriors only: retreating means dueling mobiles, never chewing
    // masonry while the real fight closes in.
    val prey = Maff.minBy(unit.matchups.targetsInRange.view.filter(e =>
      e.alive && e.unitClass.isTerran && e.unitClass.isWarrior))(
      unit.pixelDistanceEdge(_))
    if (prey.isEmpty) return false
    if ( ! DragoonTerranStand.shouldStand(unit.shieldWound, unit.cooldownLeft, terranInRange = true)) return false

    unit.agent.act("DragoonStand")
    Commander.attack(unit, prey.get)
    true
  }
}
