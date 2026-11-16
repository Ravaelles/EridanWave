package Micro.Targeting

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Time.Seconds

/**
 * Terran wall-in breach (pure gate plus game-aware pick): a fresh column
 * that stopped shooting in front of a Terran wall is not outranged, it is
 * walled - so it chews the nearest wall building instead of milling.
 * Fires only on fresh shields (a battered column is losing the fight, not
 * knocking), a quiet gun (recent shots mean a duel is already on), no
 * sieged tanks anywhere (shells outrank masonry), and a building sitting
 * in a choke (five tiles off the lane is a base, not the wall).
 * Runs after WallBreachTarget in Combat.acquireTarget: the reachability
 * breach answers unwalkable goals, this one answers the quiet column.
 */
object BreakTerranWallIn {

  /** Fresh shields only, in hit points: a chewed column retreats instead. */
  val maxShieldWound: Int = 8

  /** Quiet gun, in frames: recent shots mean a duel is already on. */
  val quietFrames: Int = Seconds(2)()

  /** Wall buildings sit in the lane: this close to a choke, in tiles. */
  val chokeRadiusTiles: Double = 5.0

  def shouldBreak(
    shieldWound: Int,
    seesTerranBuilding: Boolean,
    framesSinceAttack: Int,
    enemyHasSiegedTank: Boolean,
    distBuildingToChokeTiles: Double): Boolean =
    shieldWound <= maxShieldWound &&
      seesTerranBuilding &&
      framesSinceAttack > quietFrames &&
      ! enemyHasSiegedTank &&
      distBuildingToChokeTiles <= chokeRadiusTiles

  def run(unit: FriendlyUnitInfo): Unit = {
    if ( ! unit.unitClass.isWarrior || unit.flying || unit.loaded || ! unit.tile.valid) return
    if (unit.shieldWound > maxShieldWound) return
    if (With.framesSince(unit.lastFrameStartingAttack) <= quietFrames) return
    if (With.units.enemy.exists(e => e.alive && Terran.SiegeTankSieged(e))) return
    // A visible reachable duel still outranks masonry: a flanker behind us
    // is the fight, the wall is not.
    val dueling = unit.agent.toAttack.exists(t =>
      t.visible && t.tile.valid && With.paths.groundPathExists(unit.tile, t.tile))
    if (dueling) return

    val walls = With.units.enemy.view.filter(e =>
      e.alive
        && e.visible
        && e.unitClass.isBuilding
        && e.unitClass.isTerran).toSeq
    if (walls.isEmpty) return
    if (With.geography.edges.isEmpty) return

    val breaching = walls.view.map(wall =>
      (wall, Maff.minBy(With.geography.edges)(_.pixelCenter.pixelDistance(wall.pixel))
        .map(edge => edge.pixelCenter.pixelDistance(wall.pixel) * Maff.inv32)
        .getOrElse(Double.PositiveInfinity)))
      .filter(_._2 <= chokeRadiusTiles).toSeq
    Maff.minBy(breaching)(_._1.pixelDistanceEdge(unit)).foreach(breach => {
      unit.agent.toAttack = Some(breach._1)
    })
  }
}
