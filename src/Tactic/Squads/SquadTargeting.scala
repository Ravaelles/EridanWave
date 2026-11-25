package Tactic.Squads

import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Targeting.FiltersRequired.TargetFilterFocus
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.Group.FriendlyUnitGroup
import Utilities.?

/**
 * Squad targeting: which enemies the squad shoots. One reason to change:
 * the target strata (raid fallback to halls, en-route roadblocks, army
 * ranking).
 */
object SquadTargeting {

  def target(squad: Squad): Unit = target(squad, ?(squad.fightConsensus, squad.vicinity, squad.homeConsensus))
  def target(squad: Squad, to: Pixel): Unit = squad.setTargets(rankedEnRoute(squad, to))

  def targetRaid(squad: Squad): Unit = targetRaid(squad, squad.vicinity)
  def targetRaid(squad: Squad, to: Pixel): Unit = {
    val unrankedTargets   = unrankedAround(squad, to)
    lazy val targetCombat = unrankedTargets.filter(t => t.unitClass.isWorker || squad.canBeAttackedBy(t) || (t.unitClass.isStaticDefense && squad.units.exists(t.inRangeToAttack)))
    lazy val targetHall   = unrankedTargets.filter(_.unitClass.isTownHall)
    squad.setTargets(rankForArmy(squad, Maff.orElse(targetCombat, targetHall, unrankedTargets).toVector).sortBy( ! _.unitClass.isWorker))
  }

  def rankForArmy(squad: Squad, targets: Vector[UnitInfo]): Vector[UnitInfo] = {
    targets.sortBy(t =>
        ?(t.visible,                                  0, 32 * 5)
      + ?(t.pixel == t.pixelObserved,                 0, 32 * 5)
      + ?(t.unitClass.isWorker || squad.engagedUpon,  0, 32 * 5)
      + t.pixelDistanceCenter(squad.centroidKey)
      + t.pixelDistanceCenter(squad.vicinity))
  }

  def unrankedEnRouteTo(group: FriendlyUnitGroup, to: Pixel): Set[UnitInfo] = {
    val combatEnemiesInRoute = group.battleEnemies.filter(e =>
      ! Protoss.Interceptor(e)
      && e.likelyStillThere
      && group.canAttack(e)
      && (
        TargetFilterFocus.canTargetAsRoadblock(group, e)
        || e.matchups.engagingOn
        || e.unitClass.isStaticDefense
        || Zerg.CreepColony(e)
        || (group.canBeAttackedBy(e)
          && ! e.unitClass.isBuilding
          && group.meanAttackerSpeed > 1.2 * e.topSpeed)))
    val around = unrankedAround(group, to)
    combatEnemiesInRoute ++ around
  }

  // Larva and Eggs are never squad targets: they cannot fight, so putting
  // them on the shared list peels fighters off real targets (a Reaver even
  // prefers them through the ladder fallback). Target owns the same rule
  // per unit; the squad list applies it for the whole cluster, no exceptions.
  def unrankedAround(group: FriendlyUnitGroup, to: Pixel): Vector[UnitInfo] = {
    to.base.getOrElse(to.zone).enemies
      .filter(_.likelyStillThere)
      .filter(group.canAttack)
      .filter(_.isNone(Zerg.Larva, Zerg.Egg))
      .toVector
  }

  def rankedEnRoute(squad: Squad): Seq[UnitInfo] = rankedEnRoute(squad, squad.vicinity)
  def rankedEnRoute(squad: Squad, goalAir: Pixel): Seq[UnitInfo] = rankForArmy(squad, unrankedEnRouteTo(squad, goalAir).toVector)
  def rankedAround(squad: Squad): Seq[UnitInfo] = rankedAround(squad, squad.vicinity)
  def rankedAround(squad: Squad, goalAir: Pixel): Seq[UnitInfo] = rankForArmy(squad, unrankedAround(squad, goalAir))
}
