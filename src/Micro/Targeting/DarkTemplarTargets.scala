package Micro.Targeting

import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitClasses.UnitClass

/**
 * Pure Dark Templar target priority (no game access) - a pre-`Target`
 * doctrine pick, not a second scoring ladder (conventions rule 15):
 * `BeDarkTemplar` applies it before `Target.choose` because a Templar's
 * kill is a melee swing, not a volley the squad can share. If it ever needs
 * the squad's assignment, it becomes a `TargetFilter` instead.
 *
 * A Templar with no
 * cooldown kills what it can reach, weakest first - wounded Marauders die
 * to one swipe instead of trading with the healthy pack. Then tanks, then
 * warriors, then workers, then anything else hittable. Nearest inside
 * each tier; only real units inside closeTiles count, fogged contacts
 * keep the old nearest-hittable hunt. Distances in tiles.
 */
object DarkTemplarTargets {

  /** Weak enough to die to one swipe, in total hit points. */
  val weakHp: Int = 40

  /** Only real units this close, in tiles, enter the tiered pick. */
  val closeTiles: Double = 0.9

  /**
   * Prey only (UnitClass-level, no game access): combat buildings,
   * combat units and bases - never plain masonry or workers. A Templar
   * chewing a depot next to a tank wastes the cloak where the tank
   * answers for free. Bunkers count even though the concrete itself
   * has no gun: the marines inside are the fight.
   */
  def isPrey(unitClass: UnitClass): Boolean =
    unitClass.isCombatBuilding ||
      unitClass.isTownHall ||
      unitClass.isWarrior ||
      unitClass == Terran.Bunker

  final case class Mark(distTiles: Double, totalHealth: Int, isTank: Boolean, isWarrior: Boolean)

  def tier(mark: Mark): Int =
    if (mark.totalHealth <= weakHp) 0
    else if (mark.isTank) 1
    else if (mark.isWarrior) 2
    else 3

  /** Nearest inside the best non-empty tier; nothing when nobody qualifies. */
  def pick(marks: Seq[Mark]): Option[Int] =
    if (marks.isEmpty) None
    else Some(marks.indices.minBy(i => (tier(marks(i)), marks(i).distTiles)))
}
