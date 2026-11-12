package Micro.Actions.Protoss.Reaver

/**
 * Pure contain doctrine (no game access): our warriors facing enemy
 * defensive buildings near the main cluster means we are containing,
 * and Reavers break contains from the ground. Contain is positional,
 * not quantitative: a single warrior holding against a single static
 * counts. Vs Zerg every Reaver commits; against other races one
 * breaks while the rest stay on loose missions.
 */
object ReaverContain {

  /** More warriors than this near the cluster counts as the main force. */
  val warriorsNeeded: Int = 0

  /** This many defensive buildings inside the radius means a contain. */
  val staticsNeeded: Int = 1

  /** Contain radius around the main cluster, in tiles. */
  val staticsDistTiles: Double = 25.0

  /** Grounded Reavers break buildings inside this range, in tiles. */
  val fightDistTiles: Double = 15.0

  def containing(ourWarriors: Int, enemyStaticsNear: Int): Boolean =
    ourWarriors > warriorsNeeded && enemyStaticsNear >= staticsNeeded

  /** Release the hold only on real collapse: the army gone or the last
    * static down. With entry at single units there is no dip band to
    * ride out - zero on either side ends it. */
  val releaseWarriorsAtOrBelow: Int = 0
  val releaseStaticsAtOrBelow: Int = 0

  def holding(currentlyHolding: Boolean, ourWarriors: Int, enemyStaticsNear: Int): Boolean =
    if (currentlyHolding)
      ourWarriors > releaseWarriorsAtOrBelow && enemyStaticsNear > releaseStaticsAtOrBelow
    else
      containing(ourWarriors, enemyStaticsNear)

  def recallAll(isZerg: Boolean): Boolean =
    isZerg
}
