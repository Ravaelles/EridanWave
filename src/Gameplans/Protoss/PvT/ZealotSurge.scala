package Gameplans.Protoss.PvT

/**
 * Pure banked-Zealot surge gate (no game access): past 580 banked minerals
 * the Gates are the bottleneck, not the bank, so Zealots queue uncapped
 * until 185 supply. Below the bank or at the supply cap the normal mix
 * stands: Dragoons and casters still need their share.
 */
object ZealotSurge {

  /** Banked minerals that mean the Gates starve. */
  val bankedMinerals: Int = 580

  /** Past this supply the surge stops: save room for the rest of the mix. */
  val maxSupply: Int = 185

  def allowsMassZealots(minerals: Int, supplyUsed: Int): Boolean =
    minerals > bankedMinerals && supplyUsed < maxSupply
}
