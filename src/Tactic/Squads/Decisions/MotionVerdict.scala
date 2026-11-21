package Tactic.Squads.Decisions

import Lifecycle.PerGame

import scala.collection.mutable

/**
 * Motion verdict (pure mode order, no game access): one owner per unit
 * per frame. Withdraw outranks everything (a held retreat walks home,
 * never duels on the way out), contact outranks dressing (a lone hull
 * with a hittable duel or incoming fire holds the duel instead of
 * marching at the goal), dressing outranks the open march. Read by the
 * march, the technique and the formation from the same inputs both
 * phases already measure - the owners keep their math, the ownership
 * stops being renegotiated every frame.
 *
 * Stage 2 owns the frame: the march ticks the desired owner every frame
 * and every reader obeys the latched verdict instead of re-deriving it.
 * Upgrades preempt sharply (contact answers the same frame the march
 * sees it, in either tick order), downgrades wait out a short release -
 * a one-frame target flicker never walks the duel out of the map, and a
 * genuinely finished duel hands the march back a release later. Keyed
 * by unit id like the RetreatVerdict bus and swept per game the same way.
 */
object MotionVerdict extends PerGame {

  /** Who owns this unit's movement this frame. */
  sealed trait Mode {
    def label: String

    private[Decisions] def rank: Int
  }

  /** Walk home: the held retreat verdict owns the frame. */
  case object Withdraw extends Mode {
    override def label: String = "Withdraw"

    override private[Decisions] def rank: Int = 3
  }

  /** Duel: a hittable target or incoming fire owns the frame. */
  case object Duel extends Mode {
    override def label: String = "Duel"

    override private[Decisions] def rank: Int = 2
  }

  /** Dress: the far-wait or the pack legs own the frame. */
  case object Dress extends Mode {
    override def label: String = "Dress"

    override private[Decisions] def rank: Int = 1
  }

  /** Advance: nobody else claims the frame, walk at the goal. */
  case object Advance extends Mode {
    override def label: String = "Advance"

    override private[Decisions] def rank: Int = 0
  }

  /**
   * Downgrade release, in frames: a peaceful owner takes over only after
   * the desired side holds this long. Kills the one-frame target flicker
   * (target pick, kill edge, engaged-upon edge) while a genuine handover
   * costs at most this many frames - reaction latency dwarfs it.
   */
  val releaseFrames: Int = 3

  /**
   * Desired owner (pure flags for the suite): first claim wins, in the
   * documented order. No memory here - the latch lives in the bus below,
   * so the order itself stays a stateless function the suite pins row
   * by row.
   */
  def desired(verdictRetreat: Boolean, hasDuel: Boolean, dressing: Boolean): Mode =
    if (verdictRetreat) Withdraw
    else if (hasDuel) Duel
    else if (dressing) Dress
    else Advance

  /**
   * Handover kernel (pure numbers for the suite): a sharper owner
   * preempts the same frame (contact answers now, never after the
   * release), the same owner stands, a peaceful owner takes over only
   * past the release - the flap dies in the downgrade, never in the
   * pickup.
   */
  def owner(current: Mode, desired: Mode, stableFrames: Int): Mode =
    if (desired.rank > current.rank) desired
    else if (desired == current) current
    else if (stableFrames >= releaseFrames) desired
    else current

  private var current = new mutable.HashMap[Int, Mode]()
  private var candidate = new mutable.HashMap[Int, Mode]()
  private var candidateSince = new mutable.HashMap[Int, Int]()

  /**
   * Bus tick (one call per reader per frame): the march ticks the desired
   * owner from fresh inputs. A sharper desired mode takes over at once;
   * a peaceful one arms a candidate the release below matures. Same-frame
   * ticks share the frame, so stability counts game frames, never ticks.
   */
  def update(id: Int, mode: Mode, frame: Int): Unit = {
    val now = current.getOrElse(id, Advance)
    if (mode.rank > now.rank) {
      current(id) = mode
      candidate.remove(id)
      candidateSince.remove(id)
    } else if (mode == now) {
      candidate.remove(id)
      candidateSince.remove(id)
    } else if ( ! candidate.get(id).contains(mode)) {
      candidate(id) = mode
      candidateSince(id) = frame
    } else if (frame - candidateSince.getOrElse(id, frame) >= releaseFrames) {
      current(id) = mode
      candidate.remove(id)
      candidateSince.remove(id)
    }
  }

  /**
   * Latched owner every reader obeys: the current side, or a candidate
   * the release already matured. Unknown units advance - nobody claimed
   * the frame, so the old behavior stands.
   */
  def held(id: Int, frame: Int): Mode =
    current.get(id) match {
      case None => Advance
      case Some(now) => candidate.get(id) match {
        case Some(next) if frame - candidateSince.getOrElse(id, frame) >= releaseFrames => next
        case _ => now
      }
    }

  override def reset(): Unit = {
    current.clear()
    candidate.clear()
    candidateSince.clear()
  }
}
