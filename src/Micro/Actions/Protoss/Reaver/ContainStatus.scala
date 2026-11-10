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

  def now(): Boolean = {
    val mainCluster = With.tactics.attackSquad
    val warriors = mainCluster.units.count(_.unitClass.isWarrior)
    if (warriors <= ReaverContain.warriorsNeeded) return false

    val statics = AliveEnemies.get().count(e =>
      e.unitClass.isStaticDefense && ! e.flying && e.likelyStillThere &&
        e.pixelDistanceEdge(mainCluster.centroidGround) / 32.0 < ReaverContain.staticsDistTiles)
    ReaverContain.containing(warriors, statics)
  }

  def vsZerg(): Boolean =
    With.enemies.exists(_.isZerg)
}
