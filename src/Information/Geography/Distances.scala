package Information.Geography

import Lifecycle.With
import Mathematics.Maff

/**
 * Static map distances, memoized per deployment (no game access beyond
 * the cache key): measurements that never change inside one game on one
 * spawn, recomputed only when the map or our start changes. Pixels in,
 * tiles out.
 */
object Distances {

  private var key: String = ""
  private var naturalChokeToMainTiles: Double = 0.0

  /**
   * Natural choke to our main base, in tiles: the radius inside which a
   * hull stands its own ground instead of shuffling - reinforcements walk
   * it every game, so dodging a hot gun there wastes the volley they
   * march to join. Start-based on both ends (our spawn never moves
   * mid-game), so the value survives base trades; unknown layouts read
   * zero and disable every gate on it instead of guessing.
   */
  def naturalChokeToMainBase(): Double = {
    val now = With.mapFileName + "@" + With.self.startTile.toString
    if (now != key) {
      key = now
      naturalChokeToMainTiles = (for {
        choke <- Chokes.naturalChoke
        main  <- BaseLocations.main
      } yield choke.pixelCenter.pixelDistance(main.heart.center) * Maff.inv32)
        .getOrElse(0.0)
    }
    naturalChokeToMainTiles
  }
}
