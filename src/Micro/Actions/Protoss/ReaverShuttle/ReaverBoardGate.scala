package Micro.Actions.Protoss.ReaverShuttle

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Time.Seconds

/**
 * Quiet-crowd vocabulary (no game in the kernel): a Reaver that has not
 * shot in ten seconds with a threat inside twenty tiles mills instead
 * of fighting. The arbiter turns it into orders - no fetch chase and
 * no boarding until the hull shoots - so the brains never gate on it
 * directly. Only threats count, never scenery: an Overlord or a far
 * worker is not danger, and locking on it would pin a hull next to its
 * ride forever.
 */
object ReaverBoardGate {

  /** Silent this long, in frames: the hull is not fighting, only milling. */
  val quietFrames: Int = Seconds(10)()

  /** Crowded this close, in tiles: the guns decide the boarding. */
  val crowdRadiusTiles: Double = 20.0

  def locked(framesSinceShot: Int, nearestThreatTiles: Double): Boolean =
    framesSinceShot > quietFrames && nearestThreatTiles < crowdRadiusTiles

  /** True when this hull must stay on the ground right now. */
  def denyBoard(unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Reaver(unit)) return false

    val nearest = Maff.minBy(unit.matchups.threats.view.filter(t => t.alive && t.canAttack(unit)))(_.pixelDistanceEdge(unit))
      .map(e => unit.pixelDistanceEdge(e) * Maff.inv32)
      .getOrElse(Double.PositiveInfinity)

    locked(With.framesSince(unit.lastFrameStartingAttack), nearest)
  }
}
