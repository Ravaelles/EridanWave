package Micro.Actions.Protoss

/**
 * Pure Zealot decoy decision (no game access): a shield-stripped Zealot
 * standing with ranged allies against melee cannot win the fight, but it
 * can win the battle by fleeing instead - drawing the pack off the
 * Dragoons while they shoot unattacked. A dead Zealot helps nobody, so
 * below the shield line it withdraws to survive, not to duel.
 */
object ZealotDecoy {

  /** Below these shields a Zealot stops dueling and starts decoying. */
  val lowShields: Int = 17

  /** Allies and foes inside this range, in tiles, count for the decoy. */
  val decoyDistTiles: Double = 6.0

  def shouldDecoy(shields: Int, rangedAlliesNear: Int, meleeFoesNear: Int): Boolean =
    shields < lowShields && rangedAlliesNear >= 1 && meleeFoesNear >= 1
}
