package Debugging

import Lifecycle.PerGame

import scala.collection.mutable

/**
 * Duel-starvation tripwire (one integer per unit per frame, reported once
 * per game): counts the frames where the technique dispatch voted for the
 * duel (nothing claimed, should engage, advance allowed) but the legs
 * never ran because the unit was not ready. A healthy duel never starves;
 * a sleeping march or a legacy brain issuing orders ahead of the
 * technique does - the 1DragWounded investigation needed three hours of
 * log forensics for exactly this shape, the counter names it in one run.
 * Per-game state (reset with the rest); read the summary, not the frames.
 */
object DuelStarvation extends PerGame {

  private val starvedFrames = new mutable.HashMap[Int, Int]()
  private val classById     = new mutable.HashMap[Int, String]()

  override def reset(): Unit = {
    starvedFrames.clear()
    classById.clear()
  }

  def bump(unitId: Int, unitClass: String): Unit = {
    starvedFrames(unitId) = starvedFrames.getOrElse(unitId, 0) + 1
    classById(unitId) = unitClass
  }

  /** Top offenders for the end-of-game box, or the all-clear. */
  def summary(maxRows: Int = 3): String = {
    if (starvedFrames.isEmpty) return "### Duel starvation: none"

    val top = starvedFrames.toSeq.sortBy(-_._2).take(maxRows)
      .map { case (id, frames) => s"${classById.getOrElse(id, "?")} #$id ${frames}f" }
      .mkString(", ")
    f"### Duel starvation: $top"
  }

  /** Worst single-unit total, for suites. */
  def worstTotal: Int =
    if (starvedFrames.isEmpty) 0 else starvedFrames.values.max
}
