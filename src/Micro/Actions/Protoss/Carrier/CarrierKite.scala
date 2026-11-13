package Micro.Actions.Protoss.Carrier

/**
 * Pure dying-Carrier kite (no game access): under 100 hit points a Carrier
 * is one focus volley from the grave, so a ranged attacker that can hit it
 * inside fourteen tiles pushes the hull straight out - the Dragoon
 * KiteAway treatment for a hull that cannot afford a single trade.
 * Distances in tiles; the caller measures and steers.
 */
object CarrierKite {

  /** Below this total health the Carrier stops trading, in hit points. */
  val lowHp: Int = 100

  /** Outside this edge distance, in tiles, the Carrier holds its orders. */
  val kiteTiles: Double = 14.0

  /** Reach above which a threat counts as ranged, in pixels. */
  val rangedPixels: Double = 32.0

  def isRanged(rangePixels: Double): Boolean =
    rangePixels > rangedPixels

  def shouldKite(totalHealth: Int, distTiles: Double): Boolean =
    totalHealth < lowHp && distTiles < kiteTiles
}
