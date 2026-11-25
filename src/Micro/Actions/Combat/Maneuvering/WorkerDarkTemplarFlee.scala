package Micro.Actions.Combat.Maneuvering

import Lifecycle.With
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
 * line out, supported or not. Entry at three tiles, release a tile
 * further out, per worker, so the boundary cannot yo-yo the line.
 * Remembered blades flee too while the memory is hot - without detection
 * a blade is never sighted at all, so only fresh sightings steer.
 * Unseen damage evacuates the whole line instead: miners bleeding with
 * no visible enemy means a killer the gates cannot see, so the hall
 * latches hot and nobody returns until a full quiet window passes.
 */
object WorkerDarkTemplarFlee {

  /** Escape stride straight away from the blade, in tiles: a stride,
   * never a path. */
  val fleeTiles: Double = 10.0

  private val fleeing = new Latch
  private val hotLine = new Latch

  def fleeLatched(workerId: Int, distTiles: Double): Boolean =
    fleeing(
      workerId,
      fires    = WorkerThreatFlee.fleeDarkTemplarStraight(distTiles),
      releases = WorkerThreatFlee.releasedDarkTemplar(distTiles),
      initial  = WorkerThreatFlee.fleeDarkTemplarStraight(distTiles))

  /** Line heat, keyed by hall: miners bleeding with no visible enemy near
   * evacuate to the hall and stay until a full quiet window passes. */
  def lineHot(hallId: Int, heating: Boolean, calm: Boolean): Boolean =
    hotLine(
      hallId,
      fires    = heating,
      releases = calm)

  def run(unit: FriendlyUnitInfo): Boolean = {
    if ( ! unit.unitClass.isWorker) return false
    if (fleeTemplar(unit)) return true
    if (evacuateHotLine(unit)) return true

    false
  }

  private def fleeTemplar(unit: FriendlyUnitInfo): Boolean = {
    // Visible blades first, fresh memories second: without detection a
    // blade is never sighted, so only hot sightings steer - the damage
    // leg below owns everything older.
    val templar = Maff.minBy(unit.matchups.enemies.filter(e =>
      e.alive && Protoss.DarkTemplar(e) && (e.visible ||
        WorkerThreatFlee.freshTemplarMemory(With.framesSince(e.lastSeen)))))(
      _.pixelDistanceEdge(unit))
    val distTiles = templar
      .map(unit.pixelDistanceEdge(_) * Maff.inv32)
      .getOrElse(Double.PositiveInfinity)
    if ( ! fleeLatched(unit.id, distTiles)) return false

    // Latched true means a blade was seen or freshly remembered: the
    // release side fires on empty sightings, so the pick below always
    // exists on this branch.
    templar.exists(dt => {
      val away = unit.pixel.project(dt.pixel, -fleeTiles * 32.0).walkablePixel
      unit.agent.forceTo(away)
      unit.agent.escalatePriority(TrafficPriorities.Dodge)
      Commander.move(unit)
      unit.agent.act("WorkerDarkTemplarFlee")

      true
    })
  }

  private def evacuateHotLine(unit: FriendlyUnitInfo): Boolean = {
    val hallId = unit.base.flatMap(_.townHall).filter(_.isOurs).map(_.id)
    if (hallId.isEmpty) return false
    val miners = unit.base.map(_.ourUnits.view.filter(_.unitClass.isWorker).toSeq).getOrElse(Seq.empty)
    if (miners.isEmpty) return false

    val framesSinceDamage = miners.view.map(m => With.framesSince(m.lastFrameTakingDamage)).foldLeft(Int.MaxValue)(_ min _)
    val heating = MineralLineHeat.heating(
      minerDamagedRecently = framesSinceDamage <= MineralLineHeat.damageWindowFrames,
      visibleEnemyNear     = unit.matchups.threats.exists(t =>
        t.visible && t.pixelDistanceEdge(unit) < WorkerThreatFlee.visibleEnemyTiles * 32.0))
    if ( ! lineHot(
      hallId.get,
      heating = heating,
      calm    = MineralLineHeat.calm(framesSinceDamage))) return false

    unit.base.map(_.heart.center).foreach(heart => {
      unit.agent.forceTo(heart)
      unit.agent.escalatePriority(TrafficPriorities.Dodge)
      Commander.move(unit)
      unit.agent.act("WorkerEvacuate")
    })

    true
  }
}
