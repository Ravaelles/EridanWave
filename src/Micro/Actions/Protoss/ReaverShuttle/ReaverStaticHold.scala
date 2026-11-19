package Micro.Actions.Protoss.ReaverShuttle

import Utilities.Latch

/**
 * Static hold-to-fire (no game access): the game is bugged - a Reaver
 * closing on a Sunken or Cannon must walk inside the static's reach
 * before the scarab ever launches, even though the gun outranges it on
 * paper. So inside the launch band the hull holds every eighth frame
 * and re-attacks between holds instead of marching through: each hold
 * forces a launch attempt the march would never take. Past the band the
 * close still walks - this is only the last tiles. Entry sharp, release
 * half a tile out, per hull, so the boundary cannot dither the legs.
 * Atlantis ReaverForceHoldToFireInRange, same band, faster cadence.
 */
object ReaverStaticHold {

  /** Hold-and-ream inside this distance to the static, in tiles. */
  val holdTiles: Double = 7.25

  /** Release the band past this distance, in tiles. */
  val releaseTiles: Double = 7.75

  /** Hold one frame in this many while closing. */
  val cadenceFrames: Int = 8

  def inHold(distTiles: Double): Boolean =
    distTiles <= holdTiles

  def released(distTiles: Double): Boolean =
    distTiles > releaseTiles

  private val holding = new Latch

  def holdLatched(unitId: Int, distTiles: Double): Boolean =
    holding(
      unitId,
      fires    = inHold(distTiles),
      releases = released(distTiles),
      initial  = inHold(distTiles))

  /** Hold on these frames, re-attack between them; staggered per hull. */
  def holdFrame(frame: Int, unitId: Int): Boolean =
    (frame + unitId) % cadenceFrames == 0
}
