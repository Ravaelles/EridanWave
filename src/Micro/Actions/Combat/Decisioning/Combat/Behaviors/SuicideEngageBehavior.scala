package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Suicide-attack behavior: a suicide unit idle past ten seconds without
 * a started attack charges the nearest enemy, whatever it is and wherever
 * it stands. Runs before the generic idle rule (which needs a free shot
 * to act); together they escalate from safe to forced. Returns true when
 * it attacked or already chases the pick.
 */
object SuicideEngageBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! unit.isSuicideMission) return false

    if ( ! unit.agent.shouldFight) return false

    val enemy = Maff.minBy(unit.matchups.enemies.filter(_.alive))(_.pixelDistanceEdge(unit))
    if (enemy.isEmpty) return false

    if ( ! SuicideEngage.shouldForceAttack(With.framesSince(unit.lastFrameStartingAttack), hasEnemy = true, unit.isSuicideMission)) return false

    val target = enemy.get
    if ( ! unit.orderTarget.contains(target)) {
      unit.agent.act("SuicideEngage")
      Commander.attack(unit, target)
    }

    true
  }
}
