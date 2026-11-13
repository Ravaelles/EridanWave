package Tactic.Squads

import Information.Battles.ArmyStrength
import Information.Geography.Chokes
import Lifecycle.With
import Micro.Actions.Protoss.Reaver.ReaverContain
import ProxyBwapi.UnitTracking.AliveEnemies

/**
 * Thin game seam for the contain doctrine (ReaverContain holds the pure
 * rule): reads whether the main cluster contains right now, so unit
 * brains and missions ask one point instead of recomputing. It lives in
 * Tactic because the cluster it reads and the latch it keeps are tactic
 * state, not Reaver state; the Reaver package only borrows the answer.
 *
 * The answer is computed once per frame: the latch is state, several
 * layers ask in the same frame, and re-running the scans made the answer
 * depend on who asked first.
 */
object ContainStatus {

  private var holding: Boolean = false
  private var cachedFrame: Int = Int.MinValue
  private var cached: Boolean = false

  def reset(): Unit = {
    holding = false
    cachedFrame = Int.MinValue
    cached = false
  }

  def now(): Boolean = {
    if (cachedFrame == With.frame) return cached

    val next = holdingNow()
    holding = next
    cached = next
    cachedFrame = With.frame
    next
  }

  private def holdingNow(): Boolean = {
    val mainCluster = With.tactics.attackSquad
    val warriors = mainCluster.units.count(_.unitClass.isWarrior)
    if (warriors <= ReaverContain.warriorsNeeded && ! holding) return false

    val statics = AliveEnemies.get().count(e =>
      e.unitClass.isStaticDefense && ! e.flying && e.likelyStillThere &&
        e.tileDistanceEdge(mainCluster.centroidGround) < ReaverContain.staticsDistTiles)
    val choke = Chokes.enemyNaturalChoke
    val nearChoke = choke.exists(c => mainCluster.centroidGround.tileDistance(c.pixelCenter) <= ReaverContain.chokeContainTiles)
    val chokeStatics = choke.map(c => AliveEnemies.get().count(e =>
      e.unitClass.isStaticDefense && e.canAttackGround && ! e.flying && e.likelyStillThere &&
        e.pixel.tileDistance(c.pixelCenter) <= ReaverContain.chokeContainTiles)).getOrElse(0)
    val score = ArmyStrength.score(With.battles.globalAttack.us.skimStrengthTotal, With.battles.globalAttack.enemy.skimStrengthTotal)
    val chokeContained = ReaverContain.containingAtChoke(nearChoke, chokeStatics, score)
    val next = ReaverContain.holding(holding, warriors, statics, chokeContained)
    Debugging.Probe.note("Contain", 0, f"$next warriors=$warriors statics=$statics choke=$chokeContained")
    next
  }

  def vsZerg(): Boolean =
    With.enemies.exists(_.isZerg)
}
