package Micro.Actions.Combat.Decisioning

/**
 * Zealot pack rule (pure numbers, no game access): a small zealots-only
 * force fights as a ball, never as single duels. Below twelve hulls
 * with no other warriors around, a zealot with no packmate inside one
 * tile does not approach - it returns to the centroid instead, unless
 * the local skim runs two-to-one or better. A lone charger feeds
 * piecemeal; the pack arrives together or not at all. Already fighting
 * hulls never regroup: the rule stops lone charges into the fight, it
 * must not drag fighters out of one.
 */
object ZealotPack {

  /** Approach needs a packmate inside this radius, in tiles. */
  val packTiles: Double = 1.0

  /** Below this many zealots the ball rule applies. */
  val packSize: Int = 12

  /** ...unless the local skim runs this ratio or better. */
  val overwhelmRatio: Double = 2.0

  def shouldRegroup(
    onlyZealots: Boolean,
    zealots: Int,
    mateTiles: Double,
    usSkim: Double,
    foeSkim: Double,
    inContact: Boolean,
    enemyClosing: Boolean = false): Boolean =
    ! inContact &&
      ! enemyClosing &&
      onlyZealots &&
      zealots < packSize &&
      mateTiles > packTiles &&
      ! (foeSkim > 0 && usSkim / foeSkim >= overwhelmRatio)
}
