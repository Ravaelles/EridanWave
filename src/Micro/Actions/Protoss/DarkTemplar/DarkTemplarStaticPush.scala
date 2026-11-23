package Micro.Actions.Protoss.DarkTemplar

/**
 * Static-gun push for an unseen Dark Templar (no game access): while no
 * detector burns the tile and no shot has landed, the cloak is absolute -
 * so the hunt walks straight at the remembered gun instead of around it,
 * swiping anything hittable in range plus a 0.1-tile margin, depots and
 * forges included. The margin orders the swipe a hair early: the attack
 * walks the last sliver while the order stands. The moment the tile burns
 * or a shot lands, the standoff below owns the frame instead - the push
 * never argues with live eyes. Distances in tiles unless pixels.
 */
object DarkTemplarStaticPush {

  /** Swipe margin past the gun's own reach, in tiles. */
  val attackMarginTiles: Double = 0.1

  def inPushRange(distPixels: Double, rangePixels: Double): Boolean =
    distPixels <= rangePixels + attackMarginTiles * 32.0
}
