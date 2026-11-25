package Tactic.Defense

import Information.Battles.Types.{Division, DivisionRadius}
import Information.Geography.Types.Base
import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.UnitInfo.UnitInfo

import Utilities.UnitFilters.IsWorker
import Tactic.Squads.Group.UnitGroup

class DefenseDivision(val division: Division) extends UnitGroup {

  val enemyDistances: Vector[(UnitInfo, Double)] = division
    .enemies
    .filter(defenseDanger(_) > 0)
    .map(e => (e, defenseDistance(e)))
    .toVector

  // Our approach to choosing relevant enemies is flawed because it can cut off portions of a cluster
  val       enemiesInner : Vector[UnitInfo] = enemyDistances.view.filter(_._2 < DivisionRadius.inner).map(_._1).toVector
  lazy val  enemiesOuter : Vector[UnitInfo] = enemyDistances.view.filter(_._2 < DivisionRadius.outer).map(_._1).toVector

  val needsDefense: Boolean = enemiesInner.view.map(defenseDanger).sum >= 1.0

  lazy val base: Base =
    GetDefenseBase(division.bases
      .minBy(b => (! b.plannedExpoRecently, ! b.isOurs, ! b.isEnemy, - b.economicValue()))) // TODO: Base defense logic needs to handle case where OTHER bases need scouring and not concave in just one

  override def groupUnits: Seq[UnitInfo] = enemiesInner

  private def defenseDistance(e: UnitInfo): Double = {
    Maff.orElse(With.geography.ourBases.map(_.heart), Seq(With.geography.home)).map(e.pixelDistanceTravelling).min
  }

  private def defenseDanger(e: UnitInfo): Double = {
    // Only hulls that can hit the ground count: detectors, casters and
    // transports passing through must not raise a base squad - the
    // standing false alarm was an enemy Observer tripping the detector
    // clause. Workers scout at a discount, everything else needs a gun.
    if (IsWorker(e)) 0.35
    else if (e.unitClass.attacksGround) 1.0
    else 0.0
  }
}