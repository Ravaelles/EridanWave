package Micro.Actions.Protoss.Reaver

import Lifecycle.With
import ProxyBwapi.UnitTracking.AliveEnemies

/**
 * Thin game seam for the contain doctrine (ReaverContain holds the pure
 * rule): reads whether the main cluster contains right now, so unit
 * brains and missions ask one point instead of recomputing. Main force
 * plus defensive buildings near its centroid, per the kernel.
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
    holding = ReaverContain.holding(holding, warriors, statics)
    Debugging.Probe.note("Contain", 0, f"$holding warriors=$warriors statics=$statics")
    holding
  }

  def vsZerg(): Boolean =
    With.enemies.exists(_.isZerg)
}
