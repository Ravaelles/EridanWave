package Debugging

import Lifecycle.PerGame

import scala.collection.mutable

/**
 * Order-flap ledger (always-on alternation tripwire, reported once per
 * game): counts quick flips between locomotion (Move/AttackMove) and
 * station (Hold/Attack) orders per unit. One integer per issuance, not
 * per frame - steady orders never count, no matter how long they run.
 * A legitimate cycle (kite yield vs volley, potshot cadence) alternates
 * tens of frames apart; a flap alternates every 1-3 (VolleyHold/March
 * till death). Only the stand-vs-go pairs count: Move<->AttackMove is
 * locomotion either way, and plant volleys fire without orders at all.
 * Per-game state (reset with the rest); read the summary, not the frames.
 */
object FlapLedger extends PerGame {

  /** Frame gap inside which two different orders read as one flap. */
  val gapFrames: Int = 3

  /** Alternations past which a unit reports instead of staying quiet. */
  val thresholdFlaps: Int = 12

  sealed trait OrderKind
  case object MoveKind extends OrderKind
  case object AttackMoveKind extends OrderKind
  case object AttackKind extends OrderKind
  case object HoldKind extends OrderKind

  private val lastKind = new mutable.HashMap[Int, (OrderKind, Int)]()
  private val flaps = new mutable.HashMap[Int, Int]()
  private val classById = new mutable.HashMap[Int, String]()

  override def reset(): Unit = {
    lastKind.clear()
    flaps.clear()
    classById.clear()
  }

  private def standGoPair(a: OrderKind, b: OrderKind): Boolean =
    (a == MoveKind || a == AttackMoveKind) && (b == HoldKind || b == AttackKind) ||
      (b == MoveKind || b == AttackMoveKind) && (a == HoldKind || a == AttackKind)

  def note(unitId: Int, className: String, kind: OrderKind, frame: Int): Unit = {
    lastKind.get(unitId) match {
      case Some((last, lastFrame)) if kind != last && frame - lastFrame <= gapFrames && standGoPair(last, kind) =>
        flaps(unitId) = flaps.getOrElse(unitId, 0) + 1
      case _ =>
    }
    lastKind(unitId) = (kind, frame)
    classById(unitId) = className
  }

  private def offenders(maxRows: Int): String =
    flaps.toSeq.filter(_._2 >= thresholdFlaps).sortBy(-_._2).take(maxRows)
      .map { case (id, count) => s"${classById.getOrElse(id, "?")} #$id ${count}x" }
      .mkString(", ")

  /** Worst flapping units, or the all-clear. */
  def summary(maxRows: Int = 5): String = {
    val rows = offenders(maxRows)
    if (rows.isEmpty) "### Order flap: none"
    else "### Order flap: " + rows
  }
}
