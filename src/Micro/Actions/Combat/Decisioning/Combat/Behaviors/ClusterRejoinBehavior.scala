package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.{ClusterCohesion, MotionVerdict, SuicideMission}
import Utilities.Latch
import Utilities.?

/**
 * Cluster rejoin (one question: has this warrior drifted past fourteen
 * tiles from its centroid?): a warrior moving without a victim, neither
 * retreating nor cooling under fire, never drifts past fourteen tiles
 * from its cluster's centroid. Warriors only; the explicit orders, the
 * battle state and the contact exemptions gate below.
 */
object ClusterRejoinBehavior {

  /**
   * Operate latch (auto-registered, reset per game): the chase and the
   * leash share one actuator, so a raw eight-tile boundary trades the
   * blade every few frames instead of committing to either leg.
   */
  private val operateLatch = new Latch

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    val squad = unit.squad.getOrElse(return false)
    if (unit.agent.isScout || unit.agent.commit || With.yolo.active || SuicideMission.held(unit)) return false
    if (unit.loaded) return false
    // A committed cast walk owns the frame: yanking a Templar with a
    // picked victim back to the centroid every other frame means the
    // four-frame stand never happens and a full battery circles forever.
    // The intent expires on its own if the walk goes stale, and the
    // rejoin resumes - no stuck casters.
    if (unit.agent.spellIntent.isDefined) return false

    val centroid = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val contactTiles = 2.0
    val distTiles = unit.pixelDistanceCenter(centroid) * Maff.inv32
    val hasLocalContact = unit.matchups.enemies.exists(e => unit.pixelDistanceEdge(e) < unit.pixelRangeMax + contactTiles * 32.0)
    val verdictDuel = Tactic.Squads.Decisions.MotionVerdict.held(unit.id, With.frame) == Tactic.Squads.Decisions.MotionVerdict.Duel
    // Forced return (owner order): past eight tiles a lone chase is a
    // donation, victim or not - the hull walks home even with a victim
    // assigned instead of dueling alone a screen away. Clears the stale
    // pick so the chase cannot drag it straight back out; the release
    // band below re-arms the duel once the hull is back inside.
    if (operateLatch(unit.id,
      fires = ClusterCohesion.mustRejoin(
        isWarrior         = unit.unitClass.isWarrior,
        retreating        = combat.shouldRetreat,
        coolingUnderFire  = unit.cooldownLeft > 0 && unit.matchups.threatsInRange.nonEmpty,
        inBattle          = unit.battle.isDefined,
        macroGame         = ! With.umsMode,
        hasLocalContact   = hasLocalContact,
        verdictDuel       = verdictDuel,
        distTiles         = distTiles),
      releases = distTiles < ClusterCohesion.operateReleaseTiles)) {
      unit.agent.act("ForceRejoin")
      unit.agent.toAttack = None
      unit.agent.forceTo(centroid)
      Commander.move(unit)

      return true
    }

    if ( ! ClusterCohesion.shouldRejoin(
      isWarrior         = unit.unitClass.isWarrior,
      hasTarget         = unit.agent.toAttack.isDefined,
      retreating        = combat.shouldRetreat,
      coolingUnderFire  = unit.cooldownLeft > 0 && unit.matchups.threatsInRange.nonEmpty,
      inBattle          = unit.battle.isDefined,
      macroGame         = ! With.umsMode,
      hasLocalContact   = hasLocalContact,
      verdictDuel       = verdictDuel,
      distTiles         = distTiles)) return false

    unit.agent.act("ClusterRejoin")
    unit.agent.forceTo(centroid)
    Commander.move(unit)

    true
  }
}
