package Micro.Actions.Protoss.Reaver

import Lifecycle.With
import Information.Battles.ArmyStrength
import Information.Geography.Chokes
import ProxyBwapi.UnitTracking.AliveEnemies

/**
 * Thin game seam for the contain doctrine (ReaverContain holds the pure
 * rule): reads whether the main cluster contains right now, so unit
 * brains and missions ask one point instead of recomputing. Main force
 * plus defensive buildings near its centroid, per the kernel - or parked
 * on the enemy natural choke against anti-ground statics without
 * overwhelming force, per the choke rule.
 */
object ContainStatus {

  private var holding: Boolean = false

  def reset(): Unit = {
    holding = false
  }

  def now(): Boolean = {
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
    holding = ReaverContain.holding(holding, warriors, statics, chokeContained)
    Debugging.Probe.note("Contain", 0, f"$holding warriors=$warriors statics=$statics choke=$chokeContained")
    holding
  }

  def vsZerg(): Boolean =
    With.enemies.exists(_.isZerg)
}
