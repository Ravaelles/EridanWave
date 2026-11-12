package Tactic.Squads.Decisions

/**
 * Pure main-assault gate (no game access): a ground warrior this close, in
 * tiles, to the enemy main never retreats, holds formation or dresses the
 * line - it only attacks. Unlike SuicideCharge (Zerg main, ground-path
 * distance, which goes blind on unreachable UMS ground), this fires for any
 * race on straight tile distance, so a pack inside the enemy base never
 * poses, rallies or walks home.
 */
object MainAssault {

  /** At or below this tile distance to the enemy main the assault is on. */
  val assaultTiles: Double = 15.0

  def assaulting(distTiles: Double): Boolean =
    distTiles <= assaultTiles
}
