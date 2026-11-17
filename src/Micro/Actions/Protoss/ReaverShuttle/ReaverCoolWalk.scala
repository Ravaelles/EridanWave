package Micro.Actions.Protoss.ReaverShuttle

/**
 * Cooling walk (no game access): a Reaver that just fired holds nothing
 * by standing - the next shot is a minute away and anything closing in
 * chews a stationary hull for free. While the gun is cold (nine frames
 * or more out) a mobile threat inside 7.7 tiles is walked off instead:
 * away from the nearest gun, so the scarab line keeps its distance and
 * the next volley lands from safety. A nearly-ready gun still holds to
 * fire, and the static game keeps its own keep-off - this is only the
 * cooling duel vs walkers.
 */
object ReaverCoolWalk {

  /** Cooling threshold, in frames: a hotter gun walks, a nearly-ready one holds to fire. */
  val coolFrames: Int = 9

  /** Walk from mobile threats inside this, in tiles. */
  val walkTiles: Double = 7.7

  def shouldWalk(cooldownLeft: Int, threatTiles: Double): Boolean =
    cooldownLeft >= coolFrames && threatTiles < walkTiles
}
