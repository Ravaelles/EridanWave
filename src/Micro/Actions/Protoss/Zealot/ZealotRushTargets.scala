package Micro.Actions.Protoss.Zealot

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.UnitFilters.IsWorker

/**
 * Which unit a rushing Zealot should hit. Everything here answers "what is
 * the target"; the orders themselves belong to BeRushingZealot.
 */
object ZealotRushTargets {

  /** Lowest-health unit we can already shoot, or nothing. */
  def potshot(zealot: FriendlyUnitInfo): Option[UnitInfo] =
    Maff.minBy(zealot.matchups.targetsInRange)(_.totalHealth)

  /** Ground attackers on the map; empty means there is nothing to walk at. */
  def visibleGroundAttackers: Iterable[UnitInfo] =
    With.units.enemy.filter(t => t.unitClass.attacksGround && t.visible)

  /**
   * Outnumbered by faster melee that hunts us, the potshot is a trap:
   * stopping to shoot a 1v3 against Zerglings kills the Zealot, so it
   * rejoins the pack instead of trading.
   */
  def outnumberedByChasers(zealot: FriendlyUnitInfo): Boolean = {
    val chasers   = zealot.matchups.threatsInRange.filter(t => t.unitClass.melee && t.presumptiveTarget.contains(zealot))
    val helpNear  = zealot.matchups.allies.count(a => a.unitClass.isWarrior && a.pixelDistanceEdge(zealot) < 160)
    chasers.size > helpNear
  }

  /** The chaser worth killing: cheapest health, adjusted for the walk in. */
  def best(zealot: FriendlyUnitInfo): Option[UnitInfo] = {
    val targets = zealot.matchups.targets
      .filter(_.canAttack(zealot))
      .filter(_.visible)
      .filter(worthWalkingTo(zealot, _))
    Maff.minBy(targets)(t => t.totalHealth + 1.0 * zealot.pixelsToGetInRange(t))
  }

  /**
   * Workers are only targets once they are in the way: farming them off the
   * path wastes the rush, so a worker counts when it is at the destination
   * and working, or close enough to swing at.
   */
  private def worthWalkingTo(zealot: FriendlyUnitInfo, t: UnitInfo): Boolean = {
    ! IsWorker(t) || (
      t.metro.exists(zealot.agent.destinationFinal.pixel.get.metro.contains)
      && (t.matchups.targetsInRange.nonEmpty || t.constructing || t.gathering)
      || zealot.pixelsToGetInRange(t) < 16
    )
  }
}
