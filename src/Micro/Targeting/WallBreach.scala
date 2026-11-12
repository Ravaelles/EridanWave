package Micro.Targeting

/**
 * Pure wall-breach gate (no game access): Stardust detects a wall-off by
 * navigation reachability - a base with no ground path from the attacker's
 * side is walled, so its buildings stop being scenery and become the
 * target. Same idea here: with no ground path to the march goal the goal
 * is not the fight, the wall is. The radius keeps island maps from
 * sniping across water: only a building at arm's length is a breach.
 */
object WallBreach {

  /** Breach candidates this close, in tiles, are the wall. */
  val blockRadiusTiles: Double = 6.0

  def breaches(noGroundPath: Boolean, distTiles: Double): Boolean =
    noGroundPath && distTiles <= blockRadiusTiles
}
