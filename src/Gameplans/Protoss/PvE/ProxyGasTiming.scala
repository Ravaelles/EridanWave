package Gameplans.Protoss.PvE

/**
 * Pure proxy gas timing (no game access): a stalled Zealot proxy must gas
 * up for Dragoons once the line stands (nine Zealots out) or the bleed
 * shows (thirteen produced even if most died) - a trickle-fed line may
 * never reach thirteen alive. Hydralisks showing or the clock running out
 * force it regardless.
 */
object ProxyGasTiming {

  /** Live Zealots that mean the proxy line stands. */
  val zealotsNow: Int = 9

  /** Produced Zealots that mean the proxy bled out. */
  val zealotsEver: Int = 13

  def shouldGas(zealots: Int, zealotsTotal: Int, hydraShown: Boolean, pastDeadline: Boolean): Boolean =
    zealots >= zealotsNow || zealotsTotal >= zealotsEver || hydraShown || pastDeadline
}
