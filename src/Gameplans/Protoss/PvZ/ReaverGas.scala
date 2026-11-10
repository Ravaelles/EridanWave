package Gameplans.Protoss.PvZ

/**
 * Pure Reaver gas gate (no game access): a Reaver may drink the bank dry,
 * so production paces itself. The first Reaver always queues; the second
 * needs two-fifty banked; the third and later need three-fifty. Counts are
 * live Reavers, so losses re-open the gate.
 */
object ReaverGas {

  /** Banked gas required for the second Reaver. */
  val bankForSecond: Int = 250

  /** Banked gas required for the third Reaver and later. */
  val bankForRest: Int = 350

  def allowsReaver(reavers: Int, gas: Int): Boolean =
    if (reavers >= 2) gas >= bankForRest
    else if (reavers >= 1) gas >= bankForSecond
    else true
}
