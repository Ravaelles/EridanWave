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
  private var naturalChokeToMainBaseTiles: Double = 0.0
  private var enemyKey: String = ""
  private var naturalChokeToEnemyTiles: Double = 0.0

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
      naturalChokeToMainBaseTiles = (for {
        choke <- Chokes.naturalChoke
        main  <- BaseLocations.main
      } yield choke.pixelCenter.groundPixels(main.heart.center) * Maff.inv32)
        .getOrElse(0.0)
    }
    naturalChokeToMainBaseTiles
  }

  /**
   * Natural choke to the enemy base, in tiles: the line inside which
   * reinforcements stand their own ground. Scouted main when known,
   * enemy start otherwise - the key re-measures on discovery the same
   * way the main-side read re-measures on spawn. Unknown layouts read
   * zero and disable every gate on it instead of guessing.
   */
  def naturalChokeToEnemyBase(): Double = {
    val known = With.scouting.enemyMain.isDefined
    val now = With.mapFileName + "@" + With.self.startTile.toString + "@" + known
    if (now != enemyKey) {
      enemyKey = now
      val enemyPx = With.scouting.enemyMain.map(_.heart.center).getOrElse(With.scouting.enemyHome.center)
      naturalChokeToEnemyTiles = Chokes.naturalChoke.map(choke =>
        choke.pixelCenter.groundPixels(enemyPx) * Maff.inv32).getOrElse(0.0)
    }
    naturalChokeToEnemyTiles
  }
}
