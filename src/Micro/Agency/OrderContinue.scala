package Micro.Agency

/**
 * Order-continuation course check (no game access): re-issuing a move
 * order resets acceleration, so a far goal that merely drifted should
 * keep cruising instead of resetting every frame. Holds course only
 * past the arrival zone and inside the heading cone; near goals fall
 * back to the caller's exact point check, so kiting, potshots and
 * dodges keep their exact responsiveness - they turn constantly and
 * never cruise.
 */
object OrderContinue {

  /** Inside this cone the heading counts as unchanged, in radians. */
  val headingConeRadians: Double = Math.PI / 4

  /** Closer than this the unit is arriving: the old order coasts in. */
  val arrivalTiles: Double = 4.0

  def holdsCourse(distTiles: Double, headingAligned: Boolean): Boolean =
    distTiles > arrivalTiles && headingAligned
}
