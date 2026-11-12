package Micro.Actions.Protoss.Shuttle

import Micro.Actions.Combat.Maneuvering.AirFleeGeometry

/**
 * Cost model behind a Shuttle's errands: what a trip is worth against the
 * frames it costs. Pure arithmetic, so the priority order can be argued
 * with in a test instead of in a game.
 */
object ShuttleQuestCost {

  /** A passenger this close to enemies or targets counts as fighting.
    * Tiles live with the flee trigger in AirFleeGeometry; pixels here so
    * the engine comparisons stay integer math. */
  val fightRadiusPixels: Int = (AirFleeGeometry.fightRadiusTiles * 32).toInt

  /** Nearly-free errand: the passenger is already in reach or in danger. */
  val inReachScore: Double = 0.0001

  /** A passenger that is safe waits this many times longer for its turn. */
  val safePassengerMultiplier: Double = 10.0

  /** Frames a trip must save before the Shuttle bothers to fly it. */
  val minimumTravelFrames: Int = 24

  def multiplier(passengerFighting: Boolean, anyoneFighting: Boolean): Double =
    if (passengerFighting || ! anyoneFighting) 1.0 else safePassengerMultiplier

  def passengerScore(needsLift: Boolean): Double =
    if (needsLift) inReachScore else 1.0

  def finalCost(baseCost: Double, multiplier: Double, needsLift: Boolean): Double =
    baseCost * multiplier * passengerScore(needsLift)

  /** A shooter waits for its drop cooldown on top of the base travel time. */
  def pickupPatienceFrames(canAttack: Boolean, cooldownOnDrop: Int): Int =
    minimumTravelFrames + (if (canAttack) cooldownOnDrop else 0)
}
