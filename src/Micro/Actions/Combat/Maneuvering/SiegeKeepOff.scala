package Micro.Actions.Combat.Maneuvering

import Lifecycle.With
import Mathematics.{Maff, WoundMargin}
import Mathematics.Points.Pixel
import Micro.Agency.Commander
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Siege keep-off, one rule above every retreat plan (no game in the
 * kernel): a detected ground unit that does not want battle with sieged
 * tanks never closes inside 12.8 tiles of one, plus up to 2 wound tiles -
 * a dying hull turns at fifteen, a fresh one at thirteen. Siege range is
 * twelve, so the band always clears the shells with the eight-tenths to
 * spare for the frame the spotter blinks. The fallback steps one tile
 * past the line along the ray, recomputed every frame, so the line holds
 * instead of milling. Units that want the fight keep the TankHug
 * discipline (cooling guns walk the tank down); cloaked or burrowed
 * hulls the tanks cannot see keep their own plans.
 */
object SiegeKeepOff {

  /** Keep-off of a fresh hull, in tiles: siege range plus a blink margin. */
  val baseKeepOffTiles: Double = 12.8

  /** Extra keep-off of a fully wounded hull, in tiles. */
  val woundBonusTiles: Double = 2.0

  /** Step past the line, in tiles: the fallback lands outside, not on it. */
  val pastLineTiles: Double = 1.0

  def keepOffTiles(wound01: Double): Double =
    baseKeepOffTiles + WoundMargin(0.0, woundBonusTiles).tiles(wound01)

  def shouldYield(distTiles: Double, wound01: Double): Boolean =
    distTiles < keepOffTiles(wound01)

  def fallback(own: Pixel, threat: Pixel, keepOffTiles: Double): Pixel =
    own.project(threat, -(keepOffTiles + pastLineTiles) * 32.0)

  def act(unit: FriendlyUnitInfo): Boolean = {
    if (unit.flying) return false
    if ( ! unit.canMove) return false
    if (unit.agent.shouldFight) return false
    if ( ! unit.visibleToOpponents) return false

    val shells = With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && Terran.SiegeTankSieged(e))
    Maff.minBy(shells)(_.pixelDistanceEdge(unit)).exists(tank => {
      val keepOff = keepOffTiles(unit.woundPercent())
      if (shouldYield(unit.pixelDistanceEdge(tank) * Maff.inv32, unit.woundPercent())) {
        unit.cherryVisUnitLog("SiegeKeepOff")
        val dest = CornerAntiStuckGuard.guard(
          fallback(unit.pixel, tank.pixel, keepOff),
          With.mapPixelWidth,
          With.mapPixelHeight)
        unit.agent.forceTo(dest)
        Commander.move(unit)
        true
      } else false
    })
  }
}
