package Micro.Actions.Combat.Maneuvering

import Mathematics.Maff
import Micro.Agency.Commander
import Micro.Coordination.Pushing.TrafficPriorities
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Latch

/**
 * Worker Templar flee (thin seam over the pure thresholds): a visible
 * enemy Dark Templar on the mineral line kills miners for free, so the
 * worker runs straight away from it - no pathing detours, no waypoint
 * hops, and never through Retreat. The retreat planner walks home and
 * re-decides every frame; a blade already in range needs the shortest
 * line out, owned here. Entry at three tiles, release a tile further
 * out, per worker, so the boundary cannot yo-yo the line.
 */
object WorkerDarkTemplarFlee {

  /** Escape stride straight away from the blade, in tiles. */
  val fleeTiles: Double = 10.0

  private val fleeing = new Latch

  def fleeLatched(workerId: Int, distTiles: Double): Boolean =
    fleeing(
      workerId,
      fires    = WorkerThreatFlee.fleeDarkTemplarStraight(distTiles),
      releases = WorkerThreatFlee.releasedDarkTemplar(distTiles),
      initial  = WorkerThreatFlee.fleeDarkTemplarStraight(distTiles))

  def run(unit: FriendlyUnitInfo): Boolean = {
    if ( ! unit.unitClass.isWorker) return false

    val templar = Maff.minBy(unit.matchups.enemies.filter(e =>
      e.alive && e.visible && Protoss.DarkTemplar(e)))(
      _.pixelDistanceEdge(unit))
    val distTiles = templar
      .map(unit.pixelDistanceEdge(_) * Maff.inv32)
      .getOrElse(Double.PositiveInfinity)
    if ( ! fleeLatched(unit.id, distTiles)) return false

    // Latched true means a Templar was seen: the release side fires on
    // empty sightings, so the pick below always exists on this branch.
    templar.exists(dt => {
      val away = unit.pixel.project(dt.pixel, -fleeTiles * 32.0).walkablePixel
      unit.agent.forceTo(away)
      unit.agent.escalatePriority(TrafficPriorities.Dodge)
      Commander.move(unit)
      unit.agent.act("WorkerDarkTemplarFlee")

      true
    })
  }
}
