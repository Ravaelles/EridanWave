package Micro.Actions.Protoss.Dragoon

import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.UnitInfo

/**
 * Dragoon pack stand (no game access): three Dragoons against nothing but
 * Vultures and Terran infantry hold the line instead of retreating - light
 * Terran never outtrades a pack, running only donates the ground and the
 * stragglers. A dying hull (42 total and below) still runs. The caller
 * feeds the pack count, the light-only read over the threats, and the
 * hull's total hit points.
 */
object DragoonPackStand {

  /** Stand with more Dragoons than this in the cluster. */
  val packDragoons: Int = 2

  /** Run at or below this many total hit points. */
  val retreatHp: Int = 42

  /** The light Terran a pack outtrades: Vultures and infantry, Medics included. */
  def isLightTerran(enemy: UnitInfo): Boolean =
    enemy.isAny(Terran.Vulture, Terran.Marine, Terran.Medic, Terran.Firebat)

  /**
   * Race gate first: the pack doctrine exists against Terran today.
   * Protoss and Zerg doctrines arrive as siblings behind this gate -
   * the caller reads the race once, each race owns its numbers.
   */
  def shouldStand(
    enemyIsTerran: Boolean,
    dragoonCount: Int,
    lightOnly: Boolean,
    totalHp: Int): Boolean =
    enemyIsTerran && shouldStandVsTerran(dragoonCount, lightOnly, totalHp)

  def shouldStandVsTerran(dragoonCount: Int, lightOnly: Boolean, totalHp: Int): Boolean =
    dragoonCount > packDragoons && lightOnly && totalHp > retreatHp
}
