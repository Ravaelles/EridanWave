package Micro.Actions.Basic

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Pure worker-support rule with one game-aware seam: a miner joins a fight
 * only while a friendly warrior is actually fighting nearby. When the army
 * disengages no warrior is left fighting, so the miner withdraws too
 * instead of trading alone after the cluster has gone home.
 */
object WorkerCombatSupport {

  /** Warriors within this many tiles anchor the miner, in tiles. */
  val supportTiles: Int = 10

  val supportPixels: Double = 32.0 * supportTiles

  /**
   * Fighting hulls retire at or below this many hit points: a dying
   * worker trades the mineral line for nothing, and the staffing keeps
   * sending replacements while the fight lasts.
   */
  val fightHp: Int = 20

  def mayFight(fightingWarriorsNearby: Int, totalHealth: Int): Boolean =
    fightingWarriorsNearby > 0 && totalHealth > fightHp

  def supported(worker: FriendlyUnitInfo): Boolean = mayFight(
    worker.matchups.allies.count(_.friendly.exists(ally =>
      ally.unitClass.isWarrior
      && ! ally.unitClass.isWorker
      && ally.agent.shouldFight
      && ally.pixelDistanceEdge(worker) <= supportPixels)),
    worker.totalHealth)
}
