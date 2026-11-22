package Debugging

import Lifecycle.PerGame
import ProxyBwapi.Orders

import scala.collection.mutable

/**
 * Wasted-volley ledger (kite discipline, owner direction: never yield
 * ground for free): frames where a warrior's gun is loaded and a
 * hittable enemy stands inside its reach, yet the frame ends moving.
 * Read after the dispatch owns the frame's end: judging the
 * pre-dispatch order counts every volley fired after a step as waste,
 * even though the shot landed. Per-game state (reset with the rest);
 * read the summary, not the frames.
 */
object VolleyWaste extends PerGame {

  private val wastedFrames = new mutable.HashMap[Int, Int]()
  private val classById = new mutable.HashMap[Int, String]()
  private val orderSince = new mutable.HashMap[Int, (String, Int)]()
  private val wasteStreak = new mutable.HashMap[Int, Int]()

  override def reset(): Unit = {
    wastedFrames.clear()
    classById.clear()
    orderSince.clear()
    wasteStreak.clear()
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

  /**
   * Stale orders only (pure frames for the suite): an order takes
   * latency to apply, so the frame that issues a move past a firing
   * gun still reads Move while the volley it ordered lands. Count the
   * move only once it sticks past the application window - a glued
   * Move that never shoots.
   */
  def isStaleOrder(sinceFrame: Int, nowFrame: Int, staleFrames: Int): Boolean =
    nowFrame - sinceFrame >= staleFrames

  def bump(unitId: Int, unitClass: String, cooldownLeft: Int, hasHittable: Boolean, order: String, isWarrior: Boolean,
    frame: Int = Lifecycle.With.frame, staleFrames: Int = 3): Unit = {
    orderSince.get(unitId) match {
      case Some((lastOrder, _)) if lastOrder == order =>
      case _ => orderSince(unitId) = (order, frame)
    }
    val sinceFrame = orderSince(unitId)._2
    if ( ! isForgone(cooldownLeft, hasHittable, order, isWarrior) || ! isStaleOrder(sinceFrame, frame, staleFrames)) {
      wasteStreak.remove(unitId)

      return
    }
    // Lone flickers are order latency, not skipped volleys: a frame that
    // fires right after a step reads Move while the shot is already out.
    // Only a repeated stale Move donates volleys on purpose.
    val streak = wasteStreak.getOrElse(unitId, 0) + 1
    wasteStreak(unitId) = streak
    if (streak < 2) return

    val chain = try { Lifecycle.With.units.ours.find(_.id == unitId).map(_.agent.actions.mkString("/")).getOrElse("-") } catch { case _: Exception => "-" }
    Debugging.Probe.note("WasteDbg", unitId, s"f=" + Lifecycle.With.frame + s" cd=$cooldownLeft order=$order chain=$chain")

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
