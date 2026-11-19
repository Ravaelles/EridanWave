package Micro.Actions.Protoss.Detector

import Utilities.Latch

/**
 * Observer Templar follow (no game access): the senior eye sticks to the
 * enemy Dark Templar nearest our main instead of spreading or sightseeing
 * while a blade works the mineral line. Entry at fifty ground tiles from
 * the main - the same reach the small-army hunt walks - release six
 * tiles further out, per eye, so the edge cannot flap the coverage.
 * Survival still outranks the follow: the flee steps run first and claim
 * guns before this step is even asked.
 */
object ObserverTemplarFollow {

  /** Follow a Templar this close to our main, in ground tiles. */
  val followTiles: Double = 50.0

  /** Release the follow past this distance, in ground tiles. */
  val releaseTiles: Double = 56.0

  def inFollowRange(distTiles: Double): Boolean =
    distTiles <= followTiles

  def released(distTiles: Double): Boolean =
    distTiles > releaseTiles

  private val following = new Latch

  def followLatched(eyeId: Int, distTiles: Double): Boolean =
    following(
      eyeId,
      fires    = inFollowRange(distTiles),
      releases = released(distTiles),
      initial  = inFollowRange(distTiles))
}
