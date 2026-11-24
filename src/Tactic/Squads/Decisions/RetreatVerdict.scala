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
   * UMS opening grace, in frames (ten seconds at 24fps): even odds in
   * the first seconds of a micro map never rout the line - the UMS duel
   * is always joined, never judged before contact. Ladder games keep
   * their own verdict from frame zero.
   */
  val umsNoRetreatFrames: Int = 240

  def suppressUmsOpening(umsMode: Boolean, frame: Int): Boolean =
    umsMode && frame < umsNoRetreatFrames

  /**
   * No enemy warriors inside this, in tiles, and there is nothing to
   * retreat from: the vote stays fight no matter what the skirmish
   * verdict just said, so Reinforcements marching through empty ground
   * never route on fog flicker or stale news.
   */
  val lonelyTiles: Double = 12.0

  def lonely(nearestEnemyWarriorTiles: Double): Boolean =
    nearestEnemyWarriorTiles > lonelyTiles

  /**
   * Unified verdict (pure numbers for the suite): a warrior that may flee,
   * holds no storm cast, lost the skirmish verdict and may retreat, holds
   * the retreat - unless no enemy warrior is near enough to run from.
   * Matches the march arm where the arms agree; where they differ
   * (committed hulls, casting Templars) it sides with the technique arm
   * and the disagreement probes out against the march label.
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
    proxyRush: Boolean,
    hardCounterKnown: Boolean,
    nearestEnemyWarriorTiles: Double = 0.0): Boolean =
    isWarrior &&
      ! isReaver &&
      canFlee &&
      ! stormHold &&
      ! shouldFight &&
      ! lonely(nearestEnemyWarriorTiles) &&
      RetreatAllowed.forUnit(totalHp, unitsScore, nearestArmedTiles, proxyRush, hardCounterKnown)

  private val lastRaw = new mutable.HashMap[Int, Boolean]()
  private val fightEdgeFrame = new mutable.HashMap[Int, Int]()

  /**
   * Army-wide bus: one warrior's retreat retreats the whole army - a
   * split line feeds half its hulls into the grinder while the other
   * half walks home. Folded from the per-unit ticks below, one frame
   * behind (a frame's OR is final only once every reader ticked it).
   * Dead hulls stop ticking, so corpses fall out of the OR by
   * themselves. Same sharp-enter, timed-release shape as the unit bus.
   */
  private var armyFrame = Int.MinValue
  private var armyRaw = false
  private var closedRaw = false
  private var armyEdgeFrame = Int.MinValue

  /**
   * Retreat token (effectively private verdict): only the verdict mints
   * retreat permission, and only while the army holds the retreat. The
   * Retreating march label takes one to construct, so no other level
   * can route the army home - the compiler rejects it. Read the token
   * in the same frame as the guard that allowed the construction.
   */
  final class RetreatToken private[Decisions] ()

  object RetreatToken {
    private[Decisions] val held: RetreatToken = new RetreatToken()
  }

  def token(frame: Int): Option[RetreatToken] =
    if (heldArmy(frame)) Some(RetreatToken.held) else None

  def tokenOrThrow(frame: Int): RetreatToken =
    token(frame).getOrElse(throw new IllegalStateException("retreat label without an army verdict"))

  /**
   * Verdict tick (one call per reader per tick): records the raw verdict
   * for the unit. Both phases call this on their own schedule; the last
   * writer wins the raw side, the hold below absorbs the skew. The tick
   * also folds into the army-wide OR, closed one frame behind.
   */
  def update(id: Int, rawHold: Boolean, frame: Int): Unit = {
    if (frame != armyFrame) {
      if (armyFrame != Int.MinValue && armyRaw != closedRaw) {
        if ( ! armyRaw) armyEdgeFrame = armyFrame
        closedRaw = armyRaw
      }
      armyFrame = frame
      armyRaw = false
    }
    armyRaw ||= rawHold
    if (lastRaw.get(id).contains(rawHold)) return

    lastRaw(id) = rawHold
    if ( ! rawHold) fightEdgeFrame(id) = frame
  }

  /**
   * Retreat label (pure format for the suite): the verdict reason plus
   * both skim shares in tens, so a traceless game answers why it
   * retreated and at what odds - Retreat(OutOfContact 80vs20). Tens,
   * not units: the chain stays readable instead of exploding into a
   * distinct string per point of skim.
   */
  def retreatLabel(reason: String, usSkim: Double, foeSkim: Double): String = {
    val total = usSkim + foeSkim
    if (total <= 0) return reason

    val us = Math.round(10 * usSkim / total) * 10
    val shares = f"$us%.0fvs${100 - us}%.0f"
    if (reason.isEmpty) shares else s"$reason $shares"
  }

  /** Held verdict both readers obey: sharp enter, timed release. */
  def held(id: Int, frame: Int): Boolean =
    lastRaw.get(id) match {
      case None       => false
      case Some(raw)  => RetreatHold.held(raw, frame - fightEdgeFrame.getOrElse(id, frame), holdFrames)
    }

  /**
   * Held army verdict: the last closed frame's OR with the same timed
   * release. Every warrior retreats together or nobody does; a lone
   * corpse's stale vote never reaches a closed frame. Before the first
   * all-clear close there is no edge yet - the closed OR alone decides,
   * so an army nobody routed reads fight instead of wrapping around
   * through the minimum integer below the hold window.
   */
  def heldArmy(frame: Int): Boolean =
    if (armyEdgeFrame == Int.MinValue) closedRaw
    else RetreatHold.held(closedRaw, (frame - 1) - armyEdgeFrame, holdFrames)

  override def reset(): Unit = {
    lastRaw.clear()
    fightEdgeFrame.clear()
    armyFrame = Int.MinValue
    armyRaw = false
    closedRaw = false
    armyEdgeFrame = Int.MinValue
  }
}
