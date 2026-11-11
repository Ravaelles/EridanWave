package Lifecycle

import ProxyBwapi.UnitInfo.UnitInfo

import scala.collection.mutable

/**
 * Counts how many times our units come under attack: per-unit rising edge
 * on the live isUnderAttack flag, so one strafing run counts once no
 * matter how many frames it lasts. Reset every game; read by GameSummary.
 */
object HitCounter {

  private val hittingNow = new mutable.HashSet[Int]
  var total: Int = 0

  def reset(): Unit = {
    hittingNow.clear()
    total = 0
  }

  def onFrame(units: Iterable[UnitInfo]): Unit = {
    val hit = units.view.filter(_.underAttack).map(_.id).toSet
    total += (hit -- hittingNow).size
    hittingNow.clear()
    hittingNow ++= hit
  }
}
