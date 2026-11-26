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

  private def write(squad: Squad, targetId: Int): Unit = held(squad) = targetId

  private def clear(squad: Squad): Unit = held.remove(squad)

  /** Once per squad per frame: shared decisions note once, not per hull. */
  private def noteOnce(squad: Squad, frame: Int)(note: => Unit): Unit = {
    if ( ! countedAt.get(squad).contains(frame)) {
      countedAt(squad) = frame
      note
    }
  }

  /**
   * The pack's shared pick, end to end: hold the committed target while
   * anyone can still hit it, otherwise elect a fresh one. One home for
   * the whole decision - both focus seams call it with their bids and
   * a fresh-pick fallback, so the hold-then-elect order, the once-per-
   * squad note and the memory write cannot drift apart between callers.
   * Returns the committed target id, if any. Holding past a strictly
   * cheaper grind counts once (Low): a rising count means the hold
   * band wants review, not that the hold misfired.
   */
  def tracePack(
    squad: Squad,
    attackerId: Int,
    className: String,
    bids: Seq[SharedFocus.Bid],
    fresh: => Option[Int]): Option[Int] = {
    val held = SharedFocus.hold(read(squad).flatMap(id => bids.find(_.id == id)), bids)
    val chosen = held.map(_.id).orElse(fresh)
    // Single noteOnce for probe and ledger: a second call in the same
    // frame would see the first one's mark and stay silent.
    noteOnce(squad, Lifecycle.With.frame) {
      val mem = read(squad)
      val why = if (chosen.isEmpty) "none"
        else if (mem.isEmpty) "fresh"
        else if ( ! bids.exists(_.id == mem.get)) "dead"
        else if (bids.find(_.id == mem.get).exists(_.reach == 0)) "unreached"
        else if (chosen.contains(mem.get)) "kept"
        else "freekill"
      Debugging.Probe.note("FocusPack", attackerId, f"pack ${mem.getOrElse(-1)} chose ${chosen.getOrElse(-1)} why $why")
      held.foreach(h => {
        if (bids.exists(b => b.id != h.id && b.costToKill < h.costToKill)) {
          Debugging.ErrorLedger.note(
            "focusHeldPastBetter.at." + className,
            Debugging.ErrorLedger.Priority.Low,
            f"pack holds ${h.id} over a cheaper grind, frame ${Lifecycle.With.frame}")
        }
      })
    }
    chosen match {
      case Some(id) => write(squad, id); Some(id)
      case None     => clear(squad); None
    }
  }

  override def reset(): Unit = {
    held.clear()
    countedAt.clear()
  }
}
