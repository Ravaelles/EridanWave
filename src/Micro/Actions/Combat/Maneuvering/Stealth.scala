package Micro.Actions.Combat.Maneuvering

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Tactics.Potshots.Potshot
import Micro.Actions.Protoss.DarkTemplar.DarkTemplarBerth
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Time.Seconds
import Utilities.?

object Stealth extends Action {
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    unit.cloaked
    && unit.canMove
    && unit.intent.canFlee
    && ! With.yolo.active
    && ! unit.presumptiveTarget.exists(_.base.exists(_.owner.isUs))
    && ! unit.alliesBattle.exists(Protoss.Arbiter)
    && unit.matchups.enemies.exists(e => e.complete && ! e.unitClass.isWorker && e.attacksAgainst(unit) > 0))
  
  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    lazy val impendingTurret = unit.matchups.enemies.exists(e =>
      Protoss.PhotonCannon(e)
      && unit.pixelDistanceEdge(e) + unit.topSpeed * e.remainingCompletionFrames < e.unitClass.sightPixels + 48
      && unit.pixelDistanceEdge(e) > Protoss.Probe.dimensionMax)
    lazy val nearDetectionRange = unit.tileArea.expand(2, 2).tiles.exists(With.grids.enemyDetection.inRange)
    // Wounded Templars give mobile detectors extra room: a revealed healthy
    // hull can tank the escape, a bleeding one dies, so the berth grows to
    // eight tiles at full wounds (DarkTemplarBerth).
    lazy val woundBerthPixels = ?(Protoss.DarkTemplar(unit), DarkTemplarBerth.extraTiles(unit.woundPercent()) * 32.0, 0.0)
    lazy val nearMobileDetector = unit.matchups.groupVs.detectors.exists(u =>
      u.pixelDistanceEdge(unit) < u.sightPixels + u.topSpeed * Seconds(8)() + woundBerthPixels)
    if ( ! unit.effectivelyCloaked || nearDetectionRange || nearMobileDetector || impendingTurret) {
      Potshot.delegate(unit)
      Retreat.delegate(unit)
    }
  }
}
