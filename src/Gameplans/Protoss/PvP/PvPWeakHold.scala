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
}
