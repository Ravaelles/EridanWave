package Tactic.Missions.evaluation

/**
 * Pure harass accounting (no game access): a frame counts as worker
 * denial when any mission unit stands this close, in tiles, to an enemy
 * worker. Proximity is the honest signal - a worker that keeps mining
 * next to a raider is not denied, one that flees or dies shows up in
 * profit instead. The MissionVerdict harass credit consumes the frames.
 */
object MissionHarass {

  /** Enemy worker inside this range, in tiles, is being denied. */
  val harassDist: Double = 8.0

  def denying(distTiles: Double): Boolean =
    distTiles < harassDist
}
