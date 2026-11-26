package Tactic.Squads.Decisions

import Lifecycle.PerGame
import Lifecycle.With
import Mathematics.Maff
import Tactic.Squads.Squad
import Utilities.?

import scala.collection.mutable

/**
 * Squad formation state (one source of truth for the line drill):
 * Chaotic (no line - stragglers, post-retreat) becomes Forming once a
 * seen enemy stands inside formation reach, Forming becomes Formed once
 * the front-back depth closes to shoulder-to-shoulder, and any contact
 * (or its imminence) hands the squad to Engaged - the pack joins the
 * bite instead of posing through the grind. A lull re-reads the line
 * (back to Forming or Formed); retreat dissolves everything to Chaotic.
 * Pure transition table below, pinned in FormationReadinessSuite; the
 * thin per-squad memory beside it ticks once per frame. March and
 * technique read the state instead of re-deriving forming from their
 * own gates, so the line drills as one.
 */
object FormationReadiness extends PerGame {

  sealed trait FormState

  case object Chaotic extends FormState

  case object Forming extends FormState

  case object Formed extends FormState

  case object Engaged extends FormState

  /** Front-back depth that (re)opens the drill, in tiles: past three the line walks in detail. */
  val spreadEnterTiles: Double = 3.0

  /** Front-back depth that closes the drill, in tiles: shoulder-to-shoulder. */
  val spreadExitTiles: Double = 1.5

  /** Seen enemy inside this, in tiles, means time to form (twelve, like the dress range). */
  val foeNearTiles: Double = 12.0

  /** Sight flicker past the edge never flaps the line: release a tile and a half out. */
  val foeFarTiles: Double = 13.5

  /** Gap at or inside this, in tiles, belongs to the duel: the drill freezes, never re-votes. */
  val contactGapTiles: Double = 2.0

  /** Rear rank band, in tiles: hulls this close to the rearmost hold the anchor. */
  val rearBandTiles: Double = 1.5

  /** Forced drill, in frames (owner order): once the line starts forming
    * it drills at least this long before it may stand formed - the pack
    * meets the enemy shoulder-to-shoulder, never on the first stepped
    * frame. Contact freezes, retreat dissolves; the force binds the
    * Forming-to-Formed handover only. */
  val formForceFrames: Int = 60

  /** Safe drill, in frames (owner order): while no enemy can attack us
    * inside the safety margin the drill holds longer - there is no
    * reason to rush a safe line. */
  val formForceFramesSafe: Int = 100

  /** Safety margin, in tiles: past contact reach plus this no enemy can
    * attack us, so the drill takes the long window. */
  val safeMarginTiles: Double = 2.0

  /** Required drill length for this gap (pure numbers for the suite):
    * the long window while safe, the short one when the enemy closes. */
  def requiredFormFrames(minGapTiles: Double): Int =
    ?(minGapTiles > contactGapTiles + safeMarginTiles, formForceFramesSafe, formForceFrames)

  /**
   * Forced handover (pure numbers for the suite): a drill younger than
   * the required window stays Forming even when the depth already reads
   * formed - the line proves itself over frames, not on one step.
   */
  def enforceMinForm(current: FormState, grown: FormState, framesInForming: Int, requiredFrames: Int): FormState =
    if (current == Forming && grown == Formed && framesInForming < requiredFrames) Forming else grown

  /**
   * Commander's permission (pure flags for the suite): while the squad
   * drills pre-contact nobody votes to fight - the squad says when the
   * duel may start, never the soldier. Contact (or its imminence) hands
   * the frame back: a bitten hull fights, drill or not.
   */
  def mayFightState(current: FormState, contactAny: Boolean, minGapTiles: Double): Boolean =
    current != Forming || contactAny || minGapTiles <= contactGapTiles

  /** Fallback chase cap, in tiles: farther ahead than this the hull holds
    * and waits instead of moonwalking across the map to a lost rear. */
  val chaseTiles: Double = 6.0

  def next(
    current: FormState,
    minGapTiles: Double,
    spreadTiles: Double,
    contactAny: Boolean,
    retreating: Boolean): FormState = {
    if (retreating) return Chaotic

    // Contact is the handover, not a freeze: anyone bitten fights, and
    // the pack joins instead of posing through the grind. Imminence
    // counts the same - at two tiles the duel owns the next frames.
    if (contactAny || minGapTiles <= contactGapTiles) return Engaged

    if (minGapTiles > foeFarTiles) return Chaotic

    current match {
      case Engaged => ?(spreadTiles > spreadEnterTiles, Forming, Formed)
      case Formed  => ?(spreadTiles > spreadEnterTiles, Forming, Formed)
      case Forming => ?(spreadTiles <= spreadExitTiles, Formed, Forming)
      case Chaotic =>
        if (minGapTiles > foeNearTiles) Chaotic
        else if (spreadTiles > spreadExitTiles) Forming
        else Formed
    }
  }

  /**
   * Back-step toward the rear rank, in tiles: how far this hull walks
   * away from the foe to meet the rearmost. The rear holds the anchor,
   * the middle falls back to it, the lost (past the chase cap) hold
   * and wait instead of donating the map. Zero means hold.
   */
  def backStepTiles(myGapTiles: Double, rearGapTiles: Double): Double = {
    val ahead = rearGapTiles - myGapTiles
    if (ahead <= rearBandTiles) return 0.0

    if (ahead > chaseTiles) return 0.0

    ahead - rearBandTiles
  }

  private val memory = new mutable.HashMap[Squad, (Int, FormState, Int, Boolean, Double)]()

  def state(squad: Squad): FormState =
    memory.get(squad).map(_._2).getOrElse(Chaotic)

  /**
   * Commander's permission (thin game seam over the pure flag): while
   * this squad drills pre-contact its soldiers cast no fight votes -
   * duel brains stand down, so no Outnumbering charge overrides the
   * forming line. Squadless hulls decide for themselves, like today.
   */
  def mayFight(squad: Squad, frame: Int): Boolean = {
    val grown = update(squad, frame)
    val (_, _, _, contactAny, minGap) = memory(squad)

    mayFightState(grown, contactAny, minGap)
  }

  /**
   * Per-squad tick (thin game seam over the pure table): one scan per
   * squad per frame no matter how many hulls ask - front-back depth on
   * the goal axis (cheap tile distances, never per-foe scans), contact
   * off the precomputed matchups, nearest seen foe off the precomputed
   * threats. Warriors only: eyes and casters never own the line.
   */
  def update(squad: Squad, frame: Int): FormState = {
    memory.get(squad).filter(_._1 == frame).map(_._2).foreach(found => return found)

    val warriors = squad.units.filter(o => o.alive && o.unitClass.isWarrior)
    val coords = warriors.map(_.tileDistanceCenter(squad.vicinity))
    val spread = if (coords.size < 2) 0.0 else coords.max - coords.min
    val contactAny = warriors.exists(o => o.matchups.targetsInRange.nonEmpty || o.matchups.threatsInRange.nonEmpty)
    val minGap = warriors.flatMap(o =>
      Maff.minBy(o.matchups.threats.view.filter(t =>
        t.likelyStillThere && t.visible && t.unitClass.isWarrior))(
        o.pixelDistanceEdge(_))
        .map(o.pixelDistanceEdge(_) * Maff.inv32)).reduceOption(_ min _).getOrElse(Double.PositiveInfinity)
    val retreating = RetreatVerdict.heldArmy(frame)
    val grown = next(state(squad), minGap, spread, contactAny, retreating)
    val (_, prev, formStart, _, _) = memory.getOrElse(squad, (frame, Chaotic, frame, false, Double.PositiveInfinity))
    val started = if (prev != Forming && grown == Forming) frame else formStart
    val forced = enforceMinForm(prev, grown, frame - started, requiredFormFrames(minGap))
    memory(squad) = (frame, forced, started, contactAny, minGap)

    forced
  }

  override def reset(): Unit = {
    memory.clear()
  }
}
