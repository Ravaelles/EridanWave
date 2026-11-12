package Tactic.Squads.Decisions

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Group.FriendlyUnitGroup

/**
 * Game-coupled seam for SuicideCharge (reads the world, decides nothing):
 * land distance to the known Zerg main, per unit and per squad centroid.
 * Thin on purpose; the threshold itself stays pure and tested.
 */
object SuicideChargeSeam {

  private def mainHeart = With.scouting.enemyMain.map(_.heart.center)

  private def enemyIsZerg: Boolean =
    With.enemies.exists(_.isZerg)

  def inZone(unit: FriendlyUnitInfo): Boolean =
    ! unit.flying && unit.unitClass.isWarrior && enemyIsZerg && mainHeart.exists(heart =>
      SuicideCharge.charging(unit.pixel.groundPixels(heart) * Maff.inv32))

  def squadInZone(squad: FriendlyUnitGroup): Boolean =
    enemyIsZerg && mainHeart.exists(heart =>
      SuicideCharge.charging(squad.centroidGround.groundPixels(heart) * Maff.inv32))

  /**
   * Any race, straight tile distance: a ground warrior inside the enemy
   * main commits to the assault instead of posing or retreating. Euclidean
   * on purpose - ground-path distance goes blind where UMS ground does not
   * connect, which is exactly where packs mill around in formation.
   */
  def inMainAssault(unit: FriendlyUnitInfo): Boolean =
    ! unit.flying && unit.unitClass.isWarrior && mainHeart.exists(heart =>
      MainAssault.assaulting(unit.tileDistanceCenter(heart)))
}
