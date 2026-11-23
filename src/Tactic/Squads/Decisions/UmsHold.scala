package Tactic.Squads.Decisions

import scala.collection.mutable

/**
 * UmsHold pace (pure numbers, no game access): on UMS the line stands in
 * formation and shrinks the range instead of freezing or trickling. The
 * stride scales with the strength score (100 even, 250 dominant): even
 * or losing creeps the short step, dominant takes the long one, between
 * is linear - the stronger the line, the faster it walks. A stride
 * issues at most every third frame, and only while four in five hulls
 * stand still: the line steps as one instead of stringing out. An empty
 * line never waits for itself. Per-unit cadence state, reset per game
 * with the latch family.
 */
object UmsHold {

  /** At this score (100 * ours / foe) the line advances freely. */
  val strongScore: Double = 250.0

  /** Even score: no edge, shortest stride. */
  val evenScore: Double = 100.0

  /** Stride at or past the strong score, in tiles. */
  val fastStrideTiles: Double = 0.75

  /** Stride at or below even, in tiles. */
  val slowStrideTiles: Double = 0.15

  def strideTiles(strengthScore: Double): Double =
    if (strengthScore <= evenScore) slowStrideTiles
    else if (strengthScore >= strongScore) fastStrideTiles
    else slowStrideTiles + (fastStrideTiles - slowStrideTiles) * (strengthScore - evenScore) / (strongScore - evenScore)

  /** Stride only while this fraction of the line stands still. */
  val settleFraction: Double = 0.8

  def settled(stationary: Int, total: Int): Boolean =
    total <= 0 || stationary.toDouble / total >= settleFraction

  /** Pause between strides, in frames: a stride issues every third frame. */
  val pauseFrames: Int = 2

  private val lastStride = new mutable.HashMap[Int, Int]()

  def reset(): Unit = lastStride.clear()

  def strideNow(unitId: Int, frame: Int): Boolean = {
    val last = lastStride.getOrElse(unitId, -pauseFrames - 1)
    if (frame - last > pauseFrames) {
      lastStride(unitId) = frame

      true
    } else false
  }
}
