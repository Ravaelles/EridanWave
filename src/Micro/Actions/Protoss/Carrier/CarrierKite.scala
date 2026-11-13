package Micro.Actions.Protoss.Carrier

import Micro.Actions.Combat.Maneuvering.RangedReach

/**
 * Pure dying-Carrier kite (no game access): a Carrier with scratched
 * shields keeps its distance from ranged attackers that can hit it, on a
 * linear wound margin - fresh scratches hold close, a bleeding hull keeps
 * fifteen tiles. The Dragoon KiteAway treatment for a hull that cannot
 * afford a single trade. Distances in tiles; the caller measures and
 * steers.
 */
object CarrierKite {

  /** Wound margin at full wounds, in tiles (woundPercentBonus(0, 15)). */
  val maxMarginTiles: Double = 15.0

  def isRanged(rangePixels: Double): Boolean =
    RangedReach.isRanged(rangePixels)

  def shouldKite(shieldWounded: Boolean, distTiles: Double, marginTiles: Double): Boolean =
    shieldWounded && distTiles < marginTiles
}
