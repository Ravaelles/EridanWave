package Micro.Actions.Combat.Maneuvering

import Lifecycle.With
import Mathematics.{Maff, WoundMargin}
import Mathematics.Points.Pixel
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Static-AA standoff, one rule for every air unit (no game in the kernel):
 * no flyer ever closes inside 8 tiles of an enemy gun building, plus up
 * to 5 wound tiles - a dying hull turns at thirteen, a fresh one at
 * eight. The fallback runs six tiles straight out with a two-tile random
 * square on top, so two eyes on the same ray do not stack and mill.
 * Ground shooters keep their own range discipline (a Carrier forced
 * thirteen off a Turret could never answer it); transports keep the
 * Shuttle discipline (a second movement owner flaps the ride).
 */
object AirStaticStandoff {

  /** Keep-off of a fresh hull, in tiles: outside every static gun's reach. */
  val baseKeepOffTiles: Double = 8.0

  /** Extra keep-off of a fully wounded hull, in tiles. */
  val woundBonusTiles: Double = 5.0

  /** Fallback leg, in tiles: straight out from the gun. */
  val fallbackTiles: Double = 6.0

  /** Random square half-side, in tiles: the fallback lands inside +- this. */
  val jitterTiles: Double = 2.0

  def keepOffTiles(wound01: Double): Double =
    baseKeepOffTiles + WoundMargin(0.0, woundBonusTiles).tiles(wound01)

  def shouldYield(distTiles: Double, wound01: Double): Boolean =
    distTiles < keepOffTiles(wound01)

  def fallback(own: Pixel, threat: Pixel, jitterXpx: Double, jitterYpx: Double): Pixel =
    own.project(threat, -fallbackTiles * 32.0).add(jitterXpx.toInt, jitterYpx.toInt)

  def randomJitterPx(): Double =
    (Math.random() * 2 - 1) * jitterTiles * 32.0

  def act(unit: FriendlyUnitInfo): Boolean = {
    if ( ! unit.flying || unit.unitClass.isBuilding) return false
    if (unit.canAttackGround) return false

    val guns = With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && e.unitClass.isBuilding && e.unitClass.attacksAir)
    Maff.minBy(guns)(_.pixelDistanceEdge(unit)).exists(gun => {
      if (shouldYield(unit.pixelDistanceEdge(gun) * Maff.inv32, unit.woundPercent())) {
        unit.cherryVisUnitLog("AirStaticStandoff")
        val dest = CornerAntiStuckGuard.guard(
          fallback(unit.pixel, gun.pixel, randomJitterPx(), randomJitterPx()),
          With.mapPixelWidth,
          With.mapPixelHeight)
        unit.agent.forceTo(dest)
        Commander.move(unit)
        true
      } else false
    })
  }
}
