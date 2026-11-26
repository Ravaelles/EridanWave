package Micro.Targeting

import Lifecycle.PerGame
import Tactic.Squads.Squad

import scala.collection.mutable

/**
 * Pack-level focus memory (the shared queue's home): the squad's
 * committed kill order, keyed by the squad instance itself.
 * Squad objects persist across frames (re-commissioned, never
 * rebuilt), so the latch rides the pack; a re-formed pack starts
 * with no memory and re-votes, exactly like today. Swept per game
 * with the rest of the family. Counts one ledger note per squad per
 * frame at most - every hull asks the same question and must get
 * the same answer, not four ledger rows.
 */
object FocusMemory extends PerGame {

  private val held = new mutable.HashMap[Squad, Vector[Int]]()
  private val countedAt = new mutable.HashMap[Squad, Int]()

  def read(squad: Squad): Vector[Int] = held.getOrElse(squad, Vector.empty)

  private def write(squad: Squad, targetIds: Vector[Int]): Unit = held(squad) = targetIds

  private def clear(squad: Squad): Unit = held.remove(squad)

  /** Once per squad per frame: shared decisions note once, not per hull. */
  private def noteOnce(squad: Squad, frame: Int)(note: => Unit): Unit = {
    if ( ! countedAt.get(squad).contains(frame)) {
      countedAt(squad) = frame
      note
    }
  }

  /**
   * The pack's kill queue, end to end: hold the committed order while
   * its members stay huntable, drop the dead, jump free kills to the
   * front, append newcomers behind. One home for the whole decision -
   * both focus seams call it with their bids, so order, note and
   * memory cannot drift apart between callers. Returns the queue.
   * Holding past a strictly cheaper grind counts once (Low): a rising
   * count means the hold band wants review, not that the hold misfired.
   */
  def packOrder(squad: Squad, attackerId: Int, className: String, bids: Seq[SharedFocus.Bid]): Vector[Int] = {
    val ordered = SharedFocus.orderPack(read(squad), bids, SharedFocus.packTargets)
    // Single noteOnce for probe and ledger: a second call in the same
    // frame would see the first one's mark and stay silent.
    noteOnce(squad, Lifecycle.With.frame) {
      val mem = read(squad)
      val why = if (ordered.isEmpty) "none"
        else if (mem.isEmpty) "fresh"
        else if (ordered.head != mem.head) {
          if (mem.forall(m => ! ordered.contains(m))) "dead" else "freekill"
        }
        else "kept"
      Debugging.Probe.note("FocusPack", attackerId, f"pack ${mem.headOption.getOrElse(-1)} chose ${ordered.headOption.getOrElse(-1)} why $why")
      ordered.headOption.foreach(h => {
        if (bids.exists(b => b.id != h && b.costToKill < bids.find(_.id == h).map(_.costToKill).getOrElse(Int.MaxValue))) {
          Debugging.ErrorLedger.note(
            "focusHeldPastBetter.at." + className,
            Debugging.ErrorLedger.Priority.Low,
            f"pack holds $h over a cheaper grind, frame ${Lifecycle.With.frame}")
        }
      })
    }
    if (ordered.isEmpty) clear(squad) else write(squad, ordered)

    ordered
  }

  override def reset(): Unit = {
    held.clear()
    countedAt.clear()
  }
}
