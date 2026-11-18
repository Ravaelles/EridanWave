package Micro.Actions.Protoss.Zealot

/**
 * Zealot leash (no game access): a Zealot never operates further than
 * this from our Dragoons. A lone Zealot charging three Dragoons while
 * ours fall back is not pressure, it is a donation - the line fights
 * and retreats as one, and the Zealot's job is the Dragoons' flank,
 * not a solo duel a screen away.
 */
object ZealotLeash {

  /** Farthest a Zealot may operate from our Dragoons, in tiles. */
  val leashTiles: Double = 5.5

  /**
   * Release band, in tiles: a latched Zealot walks the leash all the way
   * back inside this before the slot leg may take it again, so the two
   * legs cannot trade the unit every frame on the boundary.
   */
  val leashReleaseTiles: Double = 4.5

  /**
   * Rejoin when the nearest Dragoon is past the leash. Pure numbers for
   * the suite; the caller measures, exempts scouts/commits/suicides, and
   * walks the leg.
   */
  def shouldRejoin(nearestDragoonTiles: Double): Boolean =
    nearestDragoonTiles > leashTiles
}
