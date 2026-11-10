package Tactic.Missions.evaluation

/**
 * Pure production gate on live unit economics (no game access): a unit
 * class earns its next copy while its recent copies pay for themselves.
 * One policy fits every class through EffectivenessParams; each class
 * owns a thin adapter (see DarkTemplarEffectiveness) that feeds its
 * ledger numbers in. Order is deliberate:
 *
 * 1. never more alive than the cap, whatever the books say;
 * 2. the required minimum always builds (doctrine, not economics);
 * 3. swimming in gas, keep building while lifetime average breaks even;
 * 4. otherwise the recent window must be strictly profitable.
 */
object EffectivenessGate {

  case class EffectivenessParams(
    minAlways    : Int,
    maxAlive     : Int,
    recentWindow : Int,
    gasFlood     : Int)

  def allows(
    produced       : Int,
    alive          : Int,
    recentBalance  : Double,
    averageBalance : Double,
    gas            : Int,
    params         : EffectivenessParams): Boolean = {
    if (alive >= params.maxAlive) return false

    if (produced < params.minAlways) return true

    if (gas >= params.gasFlood && averageBalance >= 0) return true

    recentBalance > 0
  }
}
