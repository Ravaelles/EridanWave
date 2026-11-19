package Micro.Actions.Protoss.DarkTemplar

/**
 * Raid leash (no game access): a blade deep over the enemy main does
 * not trade itself for workers or lone warriors - cannons and
 * detection answer for free, and the Templar dies unconnected. Hold
 * instead of approaching: the hunt keeps the pick only once the blade
 * is already on it. Packs are real fights and stay with the bravery
 * rule; this gate only stops the solo groceries run.
 */
object DarkTemplarRaidLeash {

  /** No worker/loner hunts this deep over the enemy main, in ground tiles. */
  val mainLeashTiles: Double = 60.0

  /** A blade already on its victim duels: this close is the fight, not the raid. */
  val duelTiles: Double = 1.1

  /**
   * Hold the hunt (pure flags for the suite): workers and lone warriors
   * inside the leash are not approached past dueling distance. Anything
   * else - packs, far truss, a blade already on the victim - hunts.
   */
  def holdRaid(isWorker: Boolean, loneWarrior: Boolean, distToEnemyMainTiles: Double, distToTargetTiles: Double): Boolean =
    (isWorker || loneWarrior) &&
      distToEnemyMainTiles <= mainLeashTiles &&
      distToTargetTiles > duelTiles
}
