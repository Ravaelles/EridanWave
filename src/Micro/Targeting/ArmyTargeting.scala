package Micro.Targeting

import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Targeting.FiltersRequired.TargetFilterFocus
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.UnitInfo
import Tactic.Squads.{FriendlyUnitGroup, Squad}
import Utilities.?

/**
 * Squad-level target gathering and ranking: which enemies are worth
 * engaging en route or around a goal, and in what order. Movement and
 * formations stay in SquadAutomation; per-unit picking lives in Target.
 */
object ArmyTargeting {

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
      + ?(squad.engagedUpon,                          0, targetRank(t))
      + t.pixelDistanceCenter(squad.centroidKey)
      + t.pixelDistanceCenter(squad.vicinity))
  }

  // Combat buildings shoot back, so they outrank workers; workers outrank
  // the rest (non-combat buildings sort last with everything else);
  // everything ties while we are being attacked.
  private def targetRank(target: UnitInfo): Int =
    if (target.unitClass.isCombatBuilding) 0
    else if (target.unitClass.isWorker) 80
    else 32 * 5

  def unrankedEnRouteTo(group: FriendlyUnitGroup, to: Pixel): Set[UnitInfo] = {
    val combatEnemiesInRoute = group.battleEnemies.filter(e => targetableInRoute(group, e))
    val around = unrankedAround(group, to)
    combatEnemiesInRoute ++ around
  }

  // An enemy is worth engaging en route when we can hit it and it either
  // blocks us, already fights us, is siegeable defense, or cannot catch us.
  private def targetableInRoute(group: FriendlyUnitGroup, enemy: UnitInfo): Boolean =
    ! Protoss.Interceptor(enemy) &&
    enemy.likelyStillThere &&
    group.canAttack(enemy) &&
    worthEngaging(group, enemy)

  private def worthEngaging(group: FriendlyUnitGroup, enemy: UnitInfo): Boolean =
    TargetFilterFocus.canTargetAsRoadblock(group, enemy) ||
    enemy.matchups.engagingOn ||
    enemy.unitClass.isStaticDefense ||
    Zerg.CreepColony(enemy) ||
    outrunnableThreat(group, enemy)

  private def outrunnableThreat(group: FriendlyUnitGroup, enemy: UnitInfo): Boolean =
    group.canBeAttackedBy(enemy) &&
    ! enemy.unitClass.isBuilding &&
    group.meanAttackerSpeed > 1.2 * enemy.topSpeed

  def unrankedAround(group: FriendlyUnitGroup, to: Pixel): Vector[UnitInfo] = {
    to.base.getOrElse(to.zone).enemies
      .filter(_.likelyStillThere)
      .filter(group.canAttack)
      .toVector
  }

  def rankedEnRoute(squad: Squad): Seq[UnitInfo] = rankedEnRoute(squad, squad.vicinity)
  def rankedEnRoute(squad: Squad, goalAir: Pixel): Seq[UnitInfo] = rankForArmy(squad, unrankedEnRouteTo(squad, goalAir).toVector)
  def rankedAround(squad: Squad): Seq[UnitInfo] = rankedAround(squad, squad.vicinity)
  def rankedAround(squad: Squad, goalAir: Pixel): Seq[UnitInfo] = rankForArmy(squad, unrankedAround(squad, goalAir))
}
