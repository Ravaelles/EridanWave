package Tactic.Squads.Decisions

/**
 * Natural-choke retreat (owner order, PvZ): a handful facing Zerg does
 * not trade in the open and does not bleed home through the open map -
 * below ten hulls and two hundred skim the line falls back to the
 * natural choke and receives the wave there. Pure numbers; the march
 * terminus and both verdict arms read the same gate.
 */
object NaturalChokeRetreat {

  /** Fewer combat units than this is a handful, not a line. */
  val maxWarriors: Int = 10

  /** Below this army strength the handful cannot hold the open map. */
  val weakArmyUnits: Double = 200.0

  /** An armed enemy inside this, in tiles, is close enough to run from. */
  val foeNearTiles: Double = 12.0

  def shouldRetreat(enemyIsZerg: Boolean, ourWarriors: Int, armyUnitsScore: Double, nearestArmedTiles: Double): Boolean =
    enemyIsZerg && ourWarriors < maxWarriors && armyUnitsScore < weakArmyUnits && nearestArmedTiles <= foeNearTiles

  /**
   * Weak handful holds home (owner order, PvZ): below ten hulls and two
   * hundred skim the line does not leave the base at all - without an
   * armed enemy in sight there is nothing to retreat from, but marching
   * blind into Zerg territory donates the wave before it arrives. Same
   * count and strength as the retreat above, minus the proximity gate;
   * the consensus never outranks it, only recovery releases it.
   */
  def shouldHoldHome(enemyIsZerg: Boolean, ourWarriors: Int, armyUnitsScore: Double): Boolean =
    enemyIsZerg && ourWarriors < maxWarriors && armyUnitsScore < weakArmyUnits
}
