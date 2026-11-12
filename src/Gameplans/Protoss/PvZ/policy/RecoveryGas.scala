package Gameplans.Protoss.PvZ.policy

/**
 * Pure recovery-gas gate (no game access): a gasless line that already
 * produced a full round of warriors without killing the enemy is a failed
 * all-in, not pressure - gas up for Dragoons instead of trickling Zealots
 * forever. Same fourteen-warrior bar as the proxy rule.
 */
object RecoveryGas {

  /** Produced warriors that mean the all-in failed to kill. */
  val warriorsTotal: Int = 14

  def shouldGas(warriorsProduced: Int): Boolean =
    warriorsProduced >= warriorsTotal
}
