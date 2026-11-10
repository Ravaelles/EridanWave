package Tactic.Missions.evaluation

import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

import scala.collection.mutable

/**
 * Running resource balance per our unit: the build cost as debt at birth,
 * bounties as credit on confirmed kills. Fed from UnitTracker birth/kill
 * hooks; read by the CherryVis kill rows and, later, mission evaluation.
 */
object KillCredits {

  private val balances = new mutable.HashMap[Int, Double]
  private val order    = new mutable.HashMap[UnitClass, Vector[Int]]

  def noteBirth(unit: FriendlyUnitInfo): Unit = {
    if ( ! unit.isOurs) return

    balances(unit.id) = -UnitBounty.weightedOf(unit.unitClass)
    order(unit.unitClass) = order.getOrElse(unit.unitClass, Vector.empty) :+ unit.id
  }

  def noteKill(killerId: Int, victimClass: UnitClass): Double = {
    val total = balances.getOrElse(killerId, 0.0) + UnitBounty.weightedOf(victimClass)
    balances(killerId) = total
    total
  }

  def balanceOf(unitId: Int): Double =
    balances.getOrElse(unitId, 0.0)

  /** Mean lifetime balance over every unit of the class, live and dead. */
  def classAverageOf(unitClass: UnitClass): Double = {
    val ids = order.getOrElse(unitClass, Vector.empty)
    if (ids.isEmpty) return 0.0

    ids.map(balanceOf).sum / ids.size
  }

  /** Mean balance over the last n produced, fewer when young. */
  def recentAverageOf(unitClass: UnitClass, lastN: Int): Double = {
    val ids = order.getOrElse(unitClass, Vector.empty).takeRight(Math.max(1, lastN))
    if (ids.isEmpty) return 0.0

    ids.map(balanceOf).sum / ids.size
  }

  def reset(): Unit = {
    balances.clear()
    order.clear()
  }

  def shortName(unitClass: UnitClass): String =
    unitClass.toString.replaceFirst("^(Protoss|Terran|Zerg|Neutral)_", "")
}
