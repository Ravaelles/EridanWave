package Debugging

import Lifecycle.PerGame

import Lifecycle.With
import Mathematics.Maff
import Micro.Agency.OrderContinue
import ProxyBwapi.Orders
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Slow-alone watch (no game access for the verdict, one seam for the
 * numbers): a unit under a plain Move order that crawls far from its
 * destination with nobody nearby is the march stutter in the act -
 * no enemy to kite, no friend to dress with, yet no cruise. Capped at
 * ten notes per game; arm the SlowAlone key in probe.txt to collect
 * them, then read back with scripts/probe.py. The cap resets per game.
 */
object SlowAlone extends PerGame {

  /** Below this fraction of top speed the unit counts as crawling. */
  val slowFraction: Double = 0.5

  /** Nobody of ours or theirs inside this radius, in tiles. */
  val aloneTiles: Double = 10.0

  /** At most this many notes per game. */
  val maxNotesPerGame: Int = 10

  private var noted: Int = 0

  override def reset(): Unit = { noted = 0 }

  def shouldNote(
    moving: Boolean,
    speedNow: Double,
    topSpeed: Double,
    distTiles: Double,
    nearestAllyTiles: Double,
    nearestEnemyTiles: Double): Boolean =
    moving &&
      topSpeed > 0 &&
      speedNow < slowFraction * topSpeed &&
      distTiles > OrderContinue.arrivalTiles &&
      nearestAllyTiles > aloneTiles &&
      nearestEnemyTiles > aloneTiles

  /** Live read for the funnel: crawls far from its goal, alone. */
  def noteIfSlowAlone(unit: FriendlyUnitInfo, to: Mathematics.Points.Pixel): Unit = {
    if (unit.order != Orders.Move) return
    if (unit.topSpeed <= 0 || unit.velocity.lengthFast >= slowFraction * unit.topSpeed) return

    val distTiles = unit.pixelDistanceCenter(to) * Maff.inv32
    if (distTiles <= OrderContinue.arrivalTiles) return

    val allyDists = With.units.ours.view.filter(u => u.alive && u != unit).map(_.pixelDistanceCenter(unit) * Maff.inv32)
    val allyNear = if (allyDists.isEmpty) Double.PositiveInfinity else allyDists.min
    val enemyDists = With.units.enemy.view.filter(e => e.alive && e.visible).map(_.pixelDistanceCenter(unit) * Maff.inv32)
    val enemyNear = if (enemyDists.isEmpty) Double.PositiveInfinity else enemyDists.min
    val speedNow: Double = unit.velocity.lengthFast
    val topSpeedNow: Double = unit.topSpeed
    if ( ! shouldNote(moving = true, speedNow, topSpeedNow, distTiles, allyNear, enemyNear)) return

    if (noted >= maxNotesPerGame) return

    noted += 1
    Probe.note("SlowAlone", unit.id,
      f"frame ${With.frame} crawling ${unit.velocity.lengthFast}%.0f/${unit.topSpeed}%.0f goal ${distTiles}%.1f tiles alone")
  }
}
