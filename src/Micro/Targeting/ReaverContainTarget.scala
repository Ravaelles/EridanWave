package Micro.Targeting

/**
 * Pure Reaver contain targeting (no game access): warriors in range always
 * come first through the combat strata, but passive statics (Bunkers take
 * a minus-twelve action penalty for not fighting back) would lose to
 * workers without this. One combat bonus cancels the penalty exactly, so
 * a warrior-less Reaver breaks the bunker line instead of chasing miners.
 */
object ReaverContainTarget {

  /** Cancels the passive-in-combat penalty for defensive buildings. */
  val staticsBonus: Double = 12.0

  def bonus(isReaver: Boolean, warriorInRange: Boolean, isStaticDefense: Boolean): Double =
    if (isReaver && ! warriorInRange && isStaticDefense) staticsBonus else 0.0
}
