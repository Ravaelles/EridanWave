package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel

import scala.collection.mutable

/**
 * Flee relocation (no game access): a unit that keeps fleeing without
 * getting anywhere is cornered or herded - running the same escape line
 * only mills it in place until something kills it. Past the stuck
 * threshold the escape ray rotates ninety degrees (left or right by unit
 * id, so neighbours split instead of stacking) instead of repeating the
 * failing line. Legs that make progress re-anchor and keep running
 * straight. Per-unit state, reset per game like the other march watches.
 */
object FleeRelocation {

  /** Keep one escape line this long, in frames, before rotating. */
  val stuckFrames: Int = 72

  /** Moving at least this far, in tiles, counts as progress, not stuck. */
  val progressTiles: Double = 5.0

  private val fleeing = new mutable.HashMap[Int, (Int, Double, Double)]()

  def reset(): Unit = { fleeing.clear() }

  /**
   * Escape destination, rotated on a stuck escape: unmoved past the
   * threshold, the same line comes back rotated; a fresh escape or real
   * progress passes through and re-anchors the clock.
   */
  def relocate(unitId: Int, own: Pixel, dest: Pixel, frame: Int): Pixel = {
    fleeing.get(unitId) match {
      case Some((startFrame, ax, ay)) =>
        val movedTiles = Math.hypot(own.x - ax, own.y - ay) / 32.0
        if (movedTiles >= progressTiles) {
          fleeing.put(unitId, (frame, own.x.toDouble, own.y.toDouble))

          dest
        } else if (frame - startFrame >= stuckFrames) {
          val dx = dest.x - own.x
          val dy = dest.y - own.y
          if (unitId % 2 == 0) Pixel(own.x - dy, own.y + dx)
          else Pixel(own.x + dy, own.y - dx)
        } else dest
      case None =>
        fleeing.put(unitId, (frame, own.x.toDouble, own.y.toDouble))

        dest
    }
  }
}
