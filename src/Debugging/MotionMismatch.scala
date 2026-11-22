package Debugging

import Lifecycle.PerGame

import scala.collection.mutable

/**
 * Motion-mismatch ledger (L1 rivalry detector, one integer per unit per
 * frame, reported once per game): counts the sustained frames where the
 * technique duels while the march holds Withdraw, or the march retreats
 * while the duel is held. Single frames are release noise - the verdict
 * bus absorbs three - so only a streak past the threshold reports. A
 * healthy split never trips it: duels under Dress or Advance, retreats
 * the technique voted itself. Per-game state (reset with the rest);
 * read the summary, not the frames.
 */
object MotionMismatch extends PerGame {

  /** Consecutive frames past which a disagreement is rivalry, not release. */
  val sustainedFrames: Int = 12

  private val duelStreak = new mutable.HashMap[Int, (Int, Int)]()
  private val marchStreak = new mutable.HashMap[Int, (Int, Int)]()
  private val classById = new mutable.HashMap[Int, String]()

  override def reset(): Unit = {
    duelStreak.clear()
    marchStreak.clear()
    classById.clear()
  }

  /** Duel legs issued while Withdraw is held: the technique fights a walk-home. */
  def bumpDuel(unitId: Int, unitClass: String, frame: Int): Unit = {
    bump(duelStreak, unitId, unitClass, frame)
  }

  /** March retreats while Duel is held: the march walks a duel out of the map. */
  def bumpMarch(unitId: Int, unitClass: String, frame: Int): Unit = {
    bump(marchStreak, unitId, unitClass, frame)
  }

  private def bump(streaks: mutable.HashMap[Int, (Int, Int)], unitId: Int, unitClass: String, frame: Int): Unit = {
    val (lastFrame, streak) = streaks.getOrElse(unitId, (frame - 1, 0))
    val next = if (frame == lastFrame + 1) streak + 1 else 1
    streaks(unitId) = (frame, next)
    classById(unitId) = unitClass
  }

  private def worst(streaks: mutable.HashMap[Int, (Int, Int)]): Int =
    if (streaks.isEmpty) 0 else streaks.values.map(_._2).max

  /** Worst sustained duel-under-withdraw streak, in frames. */
  def worstDuel: Int = worst(duelStreak)

  /** Worst sustained march-under-duel streak, in frames. */
  def worstMarch: Int = worst(marchStreak)

  private def offenders(streaks: mutable.HashMap[Int, (Int, Int)], maxRows: Int): String =
    streaks.toSeq.filter(_._2._2 >= sustainedFrames).sortBy(-_._2._2).take(maxRows)
      .map { case (id, (_, frames)) => s"${classById.getOrElse(id, "?")} #$id ${frames}f" }
      .mkString(", ")

  /** Top sustained offenders per direction, or the all-clear. */
  def summary(maxRows: Int = 3): String = {
    val duel = offenders(duelStreak, maxRows)
    val march = offenders(marchStreak, maxRows)
    if (duel.isEmpty && march.isEmpty) return "### Motion mismatch: none"

    "### Motion mismatch: " +
      (if (duel.nonEmpty) s"duel-under-withdraw $duel" else "") +
      (if (duel.nonEmpty && march.nonEmpty) "; " else "") +
      (if (march.nonEmpty) s"march-under-duel $march" else "")
  }
}
