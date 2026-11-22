package Debugging

import Lifecycle.PerGame
import ProxyBwapi.Orders

import scala.collection.mutable

/**
 * Wasted-volley ledger (kite discipline, owner direction: never yield
 * ground for free): frames where a warrior's gun is loaded and a
 * hittable enemy stands inside its reach, yet the frame ends moving.
 * The read is consistently one frame behind (all proxy reads share
 * the last settled game state), so a frame counted here truly moved
 * past a loaded volley. Per-game state (reset with the rest); read
 * the summary, not the frames.
 */
object VolleyWaste extends PerGame {

  private val wastedFrames = new mutable.HashMap[Int, Int]()
  private val classById = new mutable.HashMap[Int, String]()

  override def reset(): Unit = {
    wastedFrames.clear()
    classById.clear()
  }

  /**
   * Forgone-volley predicate (pure numbers and strings for the suite):
   * a loaded gun with a hittable target that ends the frame on a
   * movement order. Attack orders shoot, holds and stops auto-fire -
   * only walking past the volley counts.
   */
  def isForgone(cooldownLeft: Int, hasHittable: Boolean, order: String, isWarrior: Boolean): Boolean =
    isWarrior && hasHittable && cooldownLeft <= 0 &&
      (order == Orders.Move || order.startsWith("MoveTo"))

  def bump(unitId: Int, unitClass: String, cooldownLeft: Int, hasHittable: Boolean, order: String, isWarrior: Boolean): Unit = {
    if ( ! isForgone(cooldownLeft, hasHittable, order, isWarrior)) return

    wastedFrames(unitId) = wastedFrames.getOrElse(unitId, 0) + 1
    classById(unitId) = unitClass
  }

  /** Top offenders for the end-of-game box, or the all-clear. */
  def summary(maxRows: Int = 3): String = {
    if (wastedFrames.isEmpty) return "### Volley waste: none"

    val top = wastedFrames.toSeq.sortBy(-_._2).take(maxRows)
      .map { case (id, frames) => s"${classById.getOrElse(id, "?")} #$id ${frames}f" }
      .mkString(", ")
    f"### Volley waste: $top"
  }

  /** Worst single-unit total, for suites. */
  def worstTotal: Int =
    if (wastedFrames.isEmpty) 0 else wastedFrames.values.max
}
