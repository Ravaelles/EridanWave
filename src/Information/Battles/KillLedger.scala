package Information.Battles

import ProxyBwapi.UnitInfo.UnitInfo

/**
 * Lifetime kill/loss ledger, Atlantis UnitsArchive parity.
 *
 * Counts what died (armies and buildings separately) and what it was worth,
 * so tactics can tell a won fight from a lost one and the UI can show it.
 * Fed from UnitTracker.kill; reset per game from UnitTracker.onStart.
 */
object KillLedger {

  var killed            : Int = 0
  var killedBuildings   : Int = 0
  var lost              : Int = 0
  var lostBuildings     : Int = 0
  var killedResources   : Int = 0
  var lostResources     : Int = 0

  def balance: Int = killedResources - lostResources

  def reset(): Unit = {
    killed            = 0
    killedBuildings   = 0
    lost              = 0
    lostBuildings     = 0
    killedResources   = 0
    lostResources     = 0
  }

  def noteDeath(unit: UnitInfo): Unit = {
    val cost      = unit.unitClass.mineralPrice + unit.unitClass.gasPrice
    val building  = unit.unitClass.isBuilding
    if (unit.isFriendly) {
      lost += 1
      if (building) lostBuildings += 1
      lostResources += cost
    } else if (unit.isEnemy) {
      killed += 1
      if (building) killedBuildings += 1
      killedResources += cost
    }
  }
}
