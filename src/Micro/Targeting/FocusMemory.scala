package Micro.Targeting

import Lifecycle.PerGame
import Tactic.Squads.Squad

import scala.collection.mutable

/**
 * Pack-level focus memory (the shared pick's home): the squad's
 * committed shared target id, keyed by the squad instance itself.
 * Squad objects persist across frames (re-commissioned, never
 * rebuilt), so the latch rides the pack; a re-formed pack starts
 * with no memory and re-votes, exactly like today. Swept per game
 * with the rest of the family. Counts one ledger note per squad per
 * frame at most - every hull asks the same question and must get
 * the same answer, not four ledger rows.
 */
object FocusMemory extends PerGame {

  private val held = new mutable.HashMap[Squad, Int]()
  private val countedAt = new mutable.HashMap[Squad, Int]()

  def read(squad: Squad): Option[Int] = held.get(squad)

  def write(squad: Squad, targetId: Int): Unit = held(squad) = targetId

  def clear(squad: Squad): Unit = held.remove(squad)

  /** Once per squad per frame: shared decisions note once, not per hull. */
  def noteOnce(squad: Squad, frame: Int)(note: => Unit): Unit = {
    if ( ! countedAt.get(squad).contains(frame)) {
      countedAt(squad) = frame
      note
    }
  }

  override def reset(): Unit = {
    held.clear()
    countedAt.clear()
  }
}
