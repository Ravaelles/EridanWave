package Tactic.Squads

/**
 * Pure contain doctrine (no game access): our warriors facing enemy
 * defensive buildings near the main cluster means we are containing.
 * Contain is positional, not quantitative: a single warrior holding
 * against a single static counts. Moved out of the old Reaver package
 * with the doctrine rebuild: the cluster it reads and the latch the
 * seam keeps are tactic state, not Reaver state.
 */
object ContainLogic {

  /** More warriors than this near the cluster counts as the main force. */
  val warriorsNeeded: Int = 0

  /** This many defensive buildings inside the radius means a contain. */
  val staticsNeeded: Int = 1

  /** Contain radius around the main cluster, in tiles. */
  val staticsDistTiles: Double = 25.0

  /** Parked on the enemy natural choke within this distance, in tiles. */
  val chokeContainTiles: Double = 20.0

  /** Above this army score a stomp is not a contain - go kill them instead. */
  val chokeContainMaxScore: Double = 300.0

  /** Grounded Reavers break buildings inside this range, in tiles. */
  val fightDistTiles: Double = 15.0

  def containing(ourWarriors: Int, enemyStaticsNear: Int): Boolean =
    ourWarriors > warriorsNeeded && enemyStaticsNear >= staticsNeeded

  /**
   * Choke contain (no game access): parked on the enemy natural choke with
   * anti-ground defense on it, without overwhelming force, means we are
   * containing - even when the per-cluster static count disagrees.
   */
  def containingAtChoke(nearChoke: Boolean, antiGroundStatics: Int, score: Double): Boolean =
    nearChoke && antiGroundStatics >= staticsNeeded && score < chokeContainMaxScore

  /** Release the hold only on real collapse: the army gone or the last
    * static down. With entry at single units there is no dip band to
    * ride out - zero on either side ends it. A held choke contain keeps
    * holding on its own terms regardless. */
  val releaseWarriorsAtOrBelow: Int = 0
  val releaseStaticsAtOrBelow: Int = 0

  def holding(currentlyHolding: Boolean, ourWarriors: Int, enemyStaticsNear: Int, chokeContained: Boolean): Boolean =
    if (currentlyHolding)
      (ourWarriors > releaseWarriorsAtOrBelow && enemyStaticsNear > releaseStaticsAtOrBelow) || chokeContained
    else
      containing(ourWarriors, enemyStaticsNear) || chokeContained

  def recallAll(isZerg: Boolean): Boolean =
    isZerg
}
