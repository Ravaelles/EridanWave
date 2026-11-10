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

  def noteBirth(unit: FriendlyUnitInfo): Unit = {
    if (unit.isOurs) balances(unit.id) = -UnitBounty.weightedOf(unit.unitClass)
  }

  def noteKill(killerId: Int, victimClass: UnitClass): Double = {
    val total = balances.getOrElse(killerId, 0.0) + UnitBounty.weightedOf(victimClass)
    balances(killerId) = total
    total
  }

  def balanceOf(unitId: Int): Double =
    balances.getOrElse(unitId, 0.0)

  def reset(): Unit =
    balances.clear()

  def shortName(unitClass: UnitClass): String =
    unitClass.toString.replaceFirst("^(Protoss|Terran|Zerg|Neutral)_", "")
}
