package Micro.Actions.Protoss.Carrier

/**
 * Carrier recharge retreat (pure numbers, no game access): a beat hull
 * with an empty shield flies home to a battery instead of trading the
 * wing away. Below half life and under twenty shields the carrier is
 * one volley from the grave; Recharge covers the last mile once it
 * shares the battery's metro, this opens the trip.
 */
object CarrierRecharge {

  /** Retreat home below this total-health fraction. */
  val retreatHealth01: Double = 0.5

  /** ...and below this many shield points. */
  val retreatShields: Int = 20

  def shouldRetreat(health01: Double, shields: Int): Boolean =
    health01 < retreatHealth01 && shields < retreatShields
}
