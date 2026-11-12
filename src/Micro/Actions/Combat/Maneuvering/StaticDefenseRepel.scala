package Micro.Actions.Combat.Maneuvering

import Mathematics.Maff
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?

/**
 * Static-defense repel: a ground unit whose squad voted not to fight never
 * sits inside defensive reach. Inside nine tiles of an enemy combat
 * building it steps radially off the building every frame instead of
 * milling at the choke - the Returning/Rallying flap at the contact edge
 * walks zealots into Cannons one at a time, and a dead zealot deals no
 * damage. Committed assault troops fight through; frenzied (or otherwise
 * engaged) squads are untouched, since their consensus says fight.
 * Pure radius for the suite; the caller gathers the building.
 */
object StaticDefenseRepel extends Action {

  /** Inside this edge distance, in tiles, the static owns the ground. */
  val repelRadiusTiles: Double = 9.0

  def shouldRepel(distTiles: Double): Boolean =
    distTiles < repelRadiusTiles

  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    unit.canMove
    && ! unit.flying
    && ! unit.loaded
    && ! unit.agent.commit
    && unit.squad.exists(! _.fightConsensus)
    && repeller(unit).isDefined)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    repeller(unit).foreach(building => {
      // Radially off the building, past the radius: holding the line at
      // nine tiles beats trading inside seven.
      val away = building.pixel.project(unit.pixel, (repelRadiusTiles + 0.5) * 32.0)
      unit.agent.act("StaticRepel")
      unit.agent.toAttack = None
      unit.agent.decision.set(?(away.walkable, away, away.walkablePixel))
      Commander.move(unit)
    })
  }

  private def repeller(unit: FriendlyUnitInfo): Option[UnitInfo] =
    Maff.minBy(unit.matchups.enemies.filter(e =>
      e.unitClass.isStaticDefense
      && e.likelyStillThere
      && shouldRepel(unit.pixelDistanceEdge(e) * Maff.inv32)))(
      unit.pixelDistanceEdge(_))
}
