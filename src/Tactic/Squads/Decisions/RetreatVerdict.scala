package Tactic.Squads.Decisions

import Lifecycle.PerGame

import scala.collection.mutable

/**
 * Unified retreat verdict (no game access): one object owning the question
 * "is this unit retreating", updated by every reader on its own tick and
 * read by every consumer. Today the march arm and the technique march arm
 * re-derive the answer from overlapping but different inputs (the technique
 * arm adds canFlee and the storm hold; the march arm ignores both), on
 * different phases, from different vintages of shouldFight - so a
 * committed hull (canFlee=false) marches Retreating while its technique
 * walks or duels, and the audit counts a mixed frame neither reader can
 * see. The kernel below is the union of both arms; the memory beside it is
 * the shared bus both phases tick (sharp enter, timed release), so a
 * verdict that flickers fight for a frame or two - or a reader that ticks
 * late under a Planning skip - cannot drag the march home and back.
 * Keyed by unit id like the Latch family and swept per game the same way.
 */
object RetreatVerdict extends PerGame {

  /** Release lag past the last fight edge, in frames (StarEngine answer). */
  val holdFrames: Int = 24

  /**
   * Unified verdict (pure numbers for the suite): a warrior that may flee,
   * holds no storm cast, lost the skirmish verdict and may retreat, holds
   * the retreat. Matches the march arm where the arms agree; where they
   * differ (committed hulls, casting Templars) it sides with the technique
   * arm and the disagreement probes out against the march label.
   */
  def shouldHold(
    isWarrior: Boolean,
    isReaver: Boolean,
    canFlee: Boolean,
    stormHold: Boolean,
    shouldFight: Boolean,
    totalHp: Int,
    unitsScore: Double,
    nearestArmedTiles: Double,
    proxyRush: Boolean): Boolean =
    isWarrior &&
      ! isReaver &&
      canFlee &&
      ! stormHold &&
      ! shouldFight &&
      RetreatAllowed.forUnit(totalHp, unitsScore, nearestArmedTiles, proxyRush)

  private val lastRaw = new mutable.HashMap[Int, Boolean]()
  private val fightEdgeFrame = new mutable.HashMap[Int, Int]()

  /**
   * Verdict tick (one call per reader per tick): records the raw verdict
   * for the unit. Both phases call this on their own schedule; the last
   * writer wins the raw side, the hold below absorbs the skew.
   */
  def update(id: Int, rawHold: Boolean, frame: Int): Unit = {
    if (lastRaw.get(id).contains(rawHold)) return

    lastRaw(id) = rawHold
    if ( ! rawHold) fightEdgeFrame(id) = frame
  }

  /** Held verdict both readers obey: sharp enter, timed release. */
  def held(id: Int, frame: Int): Boolean =
    lastRaw.get(id) match {
      case None       => false
      case Some(raw)  => RetreatHold.held(raw, frame - fightEdgeFrame.getOrElse(id, frame), holdFrames)
    }

  override def reset(): Unit = {
    lastRaw.clear()
    fightEdgeFrame.clear()
  }
}
