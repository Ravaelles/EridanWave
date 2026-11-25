package Micro.Actions.Combat.Maneuvering

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Agency.Commander
import Micro.Coordination.Pushing.TrafficPriorities
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import bwapi.BulletType

/**
 * Storm leave (pure numbers plus the bullet seam): a Psionic Storm
 * kills whatever stands in it while the volley it interrupts never
 * lands, so leaving outranks every hold, dress, volley and duel -
 * warriors and workers alike. The storm's own bullet owns the position,
 * never a memory: leaving tracks the live cell, not a ghost.
 * Distances in tiles.
 */
object AvoidStorm {

  /** Storm radius, in tiles: the cell itself. */
  val stormRadiusTiles: Double = 3.0

  /** Leave margin past the cell edge, in tiles. */
  val marginTiles: Double = 1.0

  /** Escape stride straight away from the cell, in tiles. */
  val fleeTiles: Double = 6.0

  /** Inside the cell plus margin the unit leaves, never holds. */
  def shouldLeave(distTiles: Double): Boolean =
    distTiles <= stormRadiusTiles + marginTiles

  /** Raw escape point, straight away from the live cell. */
  def awayFrom(own: Pixel, storm: Pixel): Pixel =
    own.project(storm, -fleeTiles * 32.0)

  def act(unit: FriendlyUnitInfo): Boolean = {
    if ( ! unit.canMove) return false
    Maff.minBy(With.bullets.all.view.filter(_.bulletType == BulletType.Psionic_Storm))(
      b => unit.pixel.pixelDistance(b.pixel)).exists(storm => {
      if ( ! shouldLeave(unit.pixel.pixelDistance(storm.pixel) * Maff.inv32)) return false
      unit.agent.forceTo(awayFrom(unit.pixel, storm.pixel).walkablePixel)
      unit.agent.escalatePriority(TrafficPriorities.Dodge)
      Commander.move(unit)
      unit.agent.act("AvoidStorm")

      true
    })
  }
}
