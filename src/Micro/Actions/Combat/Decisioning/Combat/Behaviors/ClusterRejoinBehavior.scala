package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.{ClusterCohesion, MotionVerdict, SuicideMission}
import Utilities.?

/**
 * Cluster rejoin (one question: has this warrior drifted past fourteen
 * tiles from its centroid?): a warrior moving without a victim, neither
 * retreating nor cooling under fire, never drifts past fourteen tiles
 * from its cluster's centroid. Warriors only; the explicit orders, the
 * battle state and the contact exemptions gate below.
 */
object ClusterRejoinBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    val squad = unit.squad.getOrElse(return false)
    if (unit.agent.isScout || unit.agent.commit || With.yolo.active || SuicideMission.isActive(unit)) return false
    if (unit.loaded) return false
    // A committed cast walk owns the frame: yanking a Templar with a
    // picked victim back to the centroid every other frame means the
    // four-frame stand never happens and a full battery circles forever.
    // The intent expires on its own if the walk goes stale, and the
    // rejoin resumes - no stuck casters.
    if (unit.agent.spellIntent.isDefined) return false

    val centroid = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val contactTiles = 2.0
    if ( ! ClusterCohesion.shouldRejoin(
      isWarrior         = unit.unitClass.isWarrior,
      hasTarget         = unit.agent.toAttack.isDefined,
      retreating        = combat.shouldRetreat,
      coolingUnderFire  = unit.cooldownLeft > 0 && unit.matchups.threatsInRange.nonEmpty,
      inBattle          = unit.battle.isDefined,
      macroGame         = ! With.umsMode,
      hasLocalContact   = unit.matchups.enemies.exists(e => unit.pixelDistanceEdge(e) < unit.pixelRangeMax + contactTiles * 32.0),
      verdictDuel       = Tactic.Squads.Decisions.MotionVerdict.held(unit.id, With.frame) == Tactic.Squads.Decisions.MotionVerdict.Duel,
      distTiles         = unit.pixelDistanceCenter(centroid) * Maff.inv32)) return false

    unit.agent.act("ClusterRejoin")
    unit.agent.forceTo(centroid)
    Commander.move(unit)

    true
  }
}
