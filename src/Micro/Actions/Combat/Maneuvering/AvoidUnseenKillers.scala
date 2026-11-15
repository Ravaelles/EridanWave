package Micro.Actions.Combat.Maneuvering

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Agency.Commander
import Micro.Coordination.Pushing.TrafficPriorities
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Shared back of unseen killers (numbers pure, steering game-coupled):
 * burrowed Lurkers and cloaked Dark Templar kill from positions we can
 * remember but not shoot, so walking the march or the duel through them
 * feeds the line for nothing. One home so the children cannot drift
 * apart on where safety starts: the nearest remembered killer inside
 * its radius turns the unit around, anything else keeps its orders.
 */
object AvoidUnseenKillers {

  def shouldAvoid(distTiles: Double, radiusTiles: Double): Boolean =
    distTiles < radiusTiles

  /** Raw escape point, straight away from the killer. */
  def awayFrom(own: Pixel, killer: Pixel, tiles: Double): Pixel =
    own.project(killer, -tiles * 32.0)

  /** Nearest remembered killer of the given kind, visible ones excluded (they are fought, not fled). */
  def nearestKiller(unit: FriendlyUnitInfo, isKiller: UnitInfo => Boolean): Option[UnitInfo] =
    Maff.minBy(unit.matchups.threats.filter(t =>
      t.likelyStillThere && ! t.visible && isKiller(t)))(
      unit.pixelDistanceEdge(_))

  /**
   * Shared dodge: warriors that cannot fly outrun the memory instead of
   * standing in it. Flyers pass over both killers untouched, and workers
   * keep their own flee - this is the combat line's rule.
   */
  def fleeFrom(unit: FriendlyUnitInfo, killer: UnitInfo, radiusTiles: Double, fleeTiles: Double, label: String): Boolean = {
    if (unit.flying || ! unit.unitClass.isWarrior) return false
    if ( ! shouldAvoid(unit.tileDistanceEdge(killer), radiusTiles)) return false

    unit.agent.forceTo(awayFrom(unit.pixel, killer.pixel, fleeTiles).walkablePixel)
    unit.agent.escalatePriority(TrafficPriorities.Dodge)
    Commander.move(unit)
    unit.agent.act(label)

    true
  }

  /** Convenience: find and flee in one call for the child brains. */
  def avoid(unit: FriendlyUnitInfo, isKiller: UnitInfo => Boolean, radiusTiles: Double, fleeTiles: Double, label: String): Boolean =
    nearestKiller(unit, isKiller).exists(killer => fleeFrom(unit, killer, radiusTiles, fleeTiles, label))
}
