package Micro.Actions.Combat.Maneuvering

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Reaver tank keep-off (no game in the kernel): a Reaver never closes
 * inside 12.5 tiles of a sieged tank, ever. The hull only hunts other
 * units - a scarab cannot answer shells, so walking the line is donating
 * the pair. No battle-verdict gate on purpose: even a fighting Reaver
 * holds this line, the Shuttle ride is the answer to tanks, not the
 * hull. No wound scaling either: the line is absolute, not a berth.
 * Boarding claims run first in BeReaver: an escaping hull boards the
 * ride instead of walking, loaded hulls are safe by construction.
 */
object ReaverTankKeepOff {

  /**
   * Never closer than this to a sieged tank, in tiles: range plus margin.
   * Deliberately 0.3 inside ArtilleryKeepOff's fresh line: this one is
   * absolute (even a fighting hull holds it), the other scales with
   * wounds and yields to a battle verdict.
   */
  val keepOffTiles: Double = 12.5

  /** Step past the line, in tiles: the fallback lands outside, not on it. */
  val pastLineTiles: Double = 1.0

  def shouldYield(distTiles: Double): Boolean =
    distTiles < keepOffTiles

  def fallback(own: Pixel, threat: Pixel): Pixel =
    own.project(threat, -(keepOffTiles + pastLineTiles) * 32.0)

  def act(unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Reaver(unit)) return false
    if ( ! unit.canMove) return false

    val shells = With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && ArtilleryKeepOff.isArtillery(e))
    Maff.minBy(shells)(_.pixelDistanceEdge(unit)).exists(tank => {
      if (shouldYield(unit.pixelDistanceEdge(tank) * Maff.inv32)) {
        unit.cherryVisUnitLog("ReaverTankKeepOff")
        val dest = CornerAntiStuckGuard.guard(
          fallback(unit.pixel, tank.pixel),
          With.mapPixelWidth,
          With.mapPixelHeight)
        unit.agent.forceTo(dest)
        Commander.move(unit)
        true
      } else false
    })
  }
}
