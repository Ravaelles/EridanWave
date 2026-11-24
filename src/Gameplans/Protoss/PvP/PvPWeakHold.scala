package Gameplans.Protoss.PvP

/**
 * Weak-holder (pure numbers for the suite): behind on army in a mirror,
 * the natural never pays for itself - it needs a defense we cannot
 * staff yet. Hold the one-base line until the count recovers instead
 * of donating a Nexus the moment it lands.
 */
object PvPWeakHold {

  /** Weak at or below this army strength, in skim units. */
  val weakArmyUnits: Double = 140.0

  /** Weak below this many complete warriors. */
  val weakWarriors: Int = 10

  def holdNatural(armyStrengthUnits: Double, warriors: Int): Boolean =
    armyStrengthUnits <= weakArmyUnits && warriors < weakWarriors

  /**
   * Pressured expansion (owner order): an enemy combat unit inside this
   * many ground tiles of our main means the natural is a donation, not
   * an economy - same shape as the weak hold above, with the owner's
   * wider army band plus the proximity gate. Ground tiles: the walk the
   * enemy still has to make, not the crow flies.
   */
  val pressuredTiles: Double = 60.0

  /** Pressured at or below this army strength, in skim units. */
  val pressuredArmyUnits: Double = 190.0

  /** Pressured at or below this many complete warriors. */
  val pressuredWarriors: Int = 10

  def holdNaturalVsPressure(armyStrengthUnits: Double, warriors: Int, nearestEnemyTiles: Double): Boolean =
    armyStrengthUnits <= pressuredArmyUnits && warriors <= pressuredWarriors && nearestEnemyTiles <= pressuredTiles
}
