package Gameplans.Eridan.PvZ

/**
 * Pure banked-Zealot gate (no game access): past 560 banked minerals with
 * two free Gateways the line is starving, not the bank, so Zealots queue
 * past their count target. Below either threshold the normal mix stands.
 */
object ZealotBank {

  /** Banked minerals that mean the line starves. */
  val bankedMinerals: Int = 560

  /** Free Gateways that mean the line can take more. */
  val freeGatewaysNeeded: Int = 2

  /** Past this supply the line is done: save room for Dragoons and casters. */
  val maxSupply: Int = 190

  def allowsExtraZealots(minerals: Int, freeGateways: Int): Boolean =
    minerals >= bankedMinerals && freeGateways >= freeGatewaysNeeded
}
