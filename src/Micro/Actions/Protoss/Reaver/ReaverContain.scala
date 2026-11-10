package Micro.Actions.Protoss.Reaver

/**
 * Pure contain doctrine (no game access): more than ten of our warriors
 * facing three-plus enemy defensive buildings inside twenty tiles of the
 * main cluster means we are containing, and Reavers break contains from
 * the ground. Vs Zerg every Reaver commits; against other races one
 * breaks while the rest stay on loose missions.
 */
object ReaverContain {

  /** More warriors than this near the cluster counts as the main force. */
  val warriorsNeeded: Int = 10

  /** This many defensive buildings inside the radius means a contain. */
  val staticsNeeded: Int = 3

  /** Contain radius around the main cluster, in tiles. */
  val staticsDistTiles: Double = 20.0

  /** Grounded Reavers break buildings inside this range, in tiles. */
  val fightDistTiles: Double = 15.0

  def containing(ourWarriors: Int, enemyStaticsNear: Int): Boolean =
    ourWarriors > warriorsNeeded && enemyStaticsNear >= staticsNeeded

  def recallAll(isZerg: Boolean): Boolean =
    isZerg
}
