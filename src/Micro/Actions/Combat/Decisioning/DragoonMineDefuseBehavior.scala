package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Dragoon mine-defuse behavior: a detected Spider Mine inside ten tiles
 * dies to dragoon fire instead of dictating the path - but only while
 * no enemy warrior can answer inside twelve tiles. Under guns the duel
 * owns the frame; defusing there trades the hull for the mine.
 */
object DragoonMineDefuseBehavior {

  def run(unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Dragoon(unit)) return false

    val mines = unit.matchups.targets.view.filter(t =>
      t.alive && t.visible && t.detected && Terran.SpiderMine(t) &&
        DragoonMineDefuse.inDefuseRange(unit.tileDistanceEdge(t))).toSeq
    if (mines.isEmpty) return false

    // The veto reads live warriors only: a remembered ghost that already
    // left must not pin the defusal while the line walks live mines.
    val nearestThreatTiles = Maff.minBy(unit.matchups.threats.view.filter(t =>
      t.alive && t.likelyStillThere && t.canAttack(unit) && ! Terran.SpiderMine(t)))(
      unit.pixelDistanceEdge(_))
      .map(unit.pixelDistanceEdge(_) * Maff.inv32)
      .getOrElse(Double.PositiveInfinity)
    if ( ! DragoonMineDefuse.safeToDefuse(nearestThreatTiles)) return false

    NearestShot.take(unit, "MineDefuse", mines)
  }
}
