package Tactic.Squads.Decisions

import Mathematics.Points.Pixel

/**
 * Pure breach detection (no game access): counts enemies inside our bases
 * so carriers can answer real raids without chasing single scouts.
 * A single enemy combat unit marks the base breached (Atlantis semantics);
 * carriers divert only on two or more ground enemies - one raider is not
 * worth a wing. Radius in tiles.
 */
object OurBaseUnderAttack {

  /** Enemies this close, in tiles, to a base heart count as inside. */
  val breachRadiusTiles: Double = 8.0

  def enemiesInside(baseHeart: Pixel, enemyPixels: Seq[Pixel], radiusTiles: Double = breachRadiusTiles): Int =
    enemyPixels.count(p => baseHeart.tile.tileDistanceFast(p.tile) <= radiusTiles)

  def atLeastSingleEnemy(baseHeart: Pixel, enemyCombatPixels: Seq[Pixel]): Boolean =
    enemiesInside(baseHeart, enemyCombatPixels) >= 1

  def atLeastTwoGroundEnemies(baseHeart: Pixel, enemyGroundPixels: Seq[Pixel]): Boolean =
    enemiesInside(baseHeart, enemyGroundPixels) >= 2
}
