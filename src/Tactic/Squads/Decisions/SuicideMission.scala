package Tactic.Squads.Decisions

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Group.FriendlyUnitGroup

/**
 * Single suicide-mission detector (renamed from SuicideChargeSeam): a ground
 * warrior inside the enemy base never retreats, holds formation or dresses
 * the line - it only attacks, workers first. Thin on purpose: the distances
 * stay in the pure tested kernel (SuicideAssault); this only
 * reads the world. assign commits the unit, isActive reads live with no
 * stored state to go stale.
 */
object SuicideMission {

  private def mainHeart = With.scouting.enemyMain.map(_.heart.center)

  private def enemyIsZerg: Boolean =
    With.enemies.exists(_.isZerg)

  /**
   * Commit the unit to the mission while it holds; true while it holds.
   */
  def assign(unit: FriendlyUnitInfo): Boolean = {
    val active = isActive(unit)

    if (active) unit.agent.commit = true

    active
  }

  /** Live read, no stored state: true while the unit sits in the enemy base. */
  def isActive(unit: FriendlyUnitInfo): Boolean =
    inZone(unit) || inMainAssault(unit)

  def squadInZone(squad: FriendlyUnitGroup): Boolean =
    enemyIsZerg && mainHeart.exists(heart =>
      SuicideAssault.charging(squad.centroidGround.groundPixels(heart) * Maff.inv32))

  private def inZone(unit: FriendlyUnitInfo): Boolean =
    ! unit.flying && unit.unitClass.isWarrior && enemyIsZerg && mainHeart.exists(heart =>
      SuicideAssault.charging(unit.pixel.groundPixels(heart) * Maff.inv32))

  /**
   * Any race, straight tile distance: a ground warrior inside the enemy
   * main commits to the assault instead of posing or retreating. Euclidean
   * on purpose - ground-path distance goes blind where UMS ground does not
   * connect, which is exactly where packs mill around in formation.
   */
  private def inMainAssault(unit: FriendlyUnitInfo): Boolean =
    ! unit.flying && unit.unitClass.isWarrior && mainHeart.exists(heart =>
      SuicideAssault.assaulting(unit.tileDistanceCenter(heart)))
}
