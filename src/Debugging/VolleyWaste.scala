package Debugging

import Lifecycle.PerGame
import ProxyBwapi.Orders

import scala.collection.mutable

/**
 * Wasted-volley ledger (kite discipline, owner direction: never yield
 * ground for free): frames where a warrior's gun is loaded and a
 * hittable enemy stands inside its reach, yet the frame ends moving -
 * or the gun stands loaded, still and silent past a full period with
 * an enemy at the edge. Read after the dispatch owns the frame's end:
 * judging the pre-dispatch order counts every volley fired after a
 * step as waste, even though the shot landed. Per-game state (reset
 * with the rest); read the summary, not the frames.
 */
object VolleyWaste extends PerGame {

  private val wastedFrames = new mutable.HashMap[Int, Int]()
  private val wastedMoveFrames = new mutable.HashMap[Int, Int]()
  private val wastedStuckFrames = new mutable.HashMap[Int, Int]()
  private val classById = new mutable.HashMap[Int, String]()
  private val orderSince = new mutable.HashMap[Int, (String, Int)]()
  private val wasteStreak = new mutable.HashMap[Int, Int]()

  override def reset(): Unit = {
    wastedFrames.clear()
    wastedMoveFrames.clear()
    wastedStuckFrames.clear()
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
   * Stuck-gun predicate (owner order: a ready gun with an enemy at the
   * edge that stands silent is a forgone volley too): loaded, enemy at
   * most half a tile past max range, standing still, and silent past a
   * full period plus latency. Judged by gun events, never by orders -
   * a hold that auto-fires never counts, whatever it is called.
   */
  val stuckMarginPixels: Double = 16.0

  def isStuckGun(
    cooldownLeft: Int,
    hasHittableNear: Boolean,
    standingStill: Boolean,
    framesSinceAttack: Int,
    periodFrames: Int,
    latencyFrames: Int,
    isWarrior: Boolean): Boolean =
    isWarrior && hasHittableNear && cooldownLeft <= 0 && standingStill &&
      framesSinceAttack > periodFrames + latencyFrames

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
    frame: Int = Lifecycle.With.frame, staleFrames: Int = 3,
    hasHittableNear: Boolean = false, standingStill: Boolean = false,
    framesSinceAttack: Int = 0, periodFrames: Int = Int.MaxValue, latencyFrames: Int = 0): Unit = {
    orderSince.get(unitId) match {
      case Some((lastOrder, _)) if lastOrder == order =>
      case _ => orderSince(unitId) = (order, frame)
    }
    val sinceFrame = orderSince(unitId)._2
    val stale = isStaleOrder(sinceFrame, frame, staleFrames)
    val forgone = isForgone(cooldownLeft, hasHittable, order, isWarrior) && stale
    val stuck = isStuckGun(cooldownLeft, hasHittableNear, standingStill, framesSinceAttack, periodFrames, latencyFrames, isWarrior) && stale
    if ( ! forgone && ! stuck) {
      wasteStreak.remove(unitId)

      return
    }
    // Lone flickers are order latency, not skipped volleys: a frame that
    // fires right after a step reads Move while the shot is already out.
    // Only a repeated stale Move donates volleys on purpose.
    val streak = wasteStreak.getOrElse(unitId, 0) + 1
    wasteStreak(unitId) = streak
    if (streak < 2) return

    wastedFrames(unitId) = wastedFrames.getOrElse(unitId, 0) + 1
    if (forgone) wastedMoveFrames(unitId) = wastedMoveFrames.getOrElse(unitId, 0) + 1
    else wastedStuckFrames(unitId) = wastedStuckFrames.getOrElse(unitId, 0) + 1
    classById(unitId) = unitClass
  }

  /** Top offenders for the end-of-game box, or the all-clear. */
  def summary(maxRows: Int = 3): String = {
    if (wastedFrames.isEmpty) return "### Volley waste: none"

    val top = wastedFrames.toSeq.sortBy(-_._2).take(maxRows)
      .map { case (id, frames) => s"${classById.getOrElse(id, "?")} #$id ${frames}f (move ${wastedMoveFrames.getOrElse(id, 0)}f, stuck ${wastedStuckFrames.getOrElse(id, 0)}f)" }
      .mkString(", ")
    f"### Volley waste: $top"
  }

  /** Worst single-unit total, for suites. */
  def worstTotal: Int =
    if (wastedFrames.isEmpty) 0 else wastedFrames.values.max
}
