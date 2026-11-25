package Micro.Agency

/**
 * Unit-level intention vocabulary (owner order, phase 1): WHY a unit wants
 * the frame. Behaviors post bids carrying a reason, never a number - the
 * Commander owns the only rank table (below) and decides. The vocabulary
 * stays open on purpose: a behavior may propose a new reason, but the
 * reason only earns its rank through one table row plus one suite row -
 * otherwise the table becomes a second ladder nobody understands.
 */
object UnitIntentionReason {

  sealed trait Reason

  /** Life-saving: retreats, evacuations, anything that owns every frame. */
  case object Survival extends Reason
  /** Damage avoided outright: storms, static shells, mines, scarabs. */
  case object Dodge extends Reason
  /** The duel: volleys, kites, charges, holds that shoot. */
  case object Duel extends Reason
  /** The dressed line: slots, holds, advances owned by the formation. */
  case object Formation extends Reason
  /** The march: squad movement while no duel owns the frame. */
  case object March extends Reason
  /** Upkeep: gather, build, repair - loses to every fight reason. */
  case object Macro extends Reason
}

/**
 * The only priority table (rule 11): reason in, rank out. Pure kernel
 * beside the Commander it serves; the suite pins every adjacency, so a
 * rank drift fails loudly instead of re-tuning the game by hand.
 */
object UnitIntentionRank {

  def rank(reason: UnitIntentionReason.Reason): Int =
    reason match {
      case UnitIntentionReason.Survival  => 5
      case UnitIntentionReason.Dodge     => 4
      case UnitIntentionReason.Duel      => 3
      case UnitIntentionReason.Formation => 2
      case UnitIntentionReason.March     => 1
      case UnitIntentionReason.Macro     => 0
    }

  /**
   * Picks the winning bid: highest rank, first posted wins ties (the
   * first-claims contract, kept as the deterministic tie-break). Returns
   * the winner's index, so identical payloads never confuse identity.
   */
  def winnerIndex(bids: Seq[UnitIntention]): Int =
    bids.indices.foldLeft(0)((best, i) =>
      if (rank(bids(i).reason) > rank(bids(best).reason)) i else best)

  /** Stable ledger key for a dropped bid: loser-vs-winner at the class. */
  def outrankedKey(loser: UnitIntentionReason.Reason, winner: UnitIntentionReason.Reason, className: String): String =
    f"outrankedBid.$loser%sVs$winner%s.at.$className%s"

  /** Stable ledger key for bids stood down before an already-issued order. */
  def stoodDownKey(directIssuer: String, className: String): String =
    f"bidStoodDown.$directIssuer%s.at.$className%s"
}

/**
 * One unit's bid for the frame: why (reason), who (issuer, for the ledger),
 * what (the order, deferred - runs once, only if this bid wins). The thunk
 * wraps the BWAPI call plus its adjacent bookkeeping (sleep, flap note);
 * everything else (destinations, votes, pushes, probes) already ran while
 * posting, exactly once. Bids live on the Agent, cleared every frame.
 */
case class UnitIntention(reason: UnitIntentionReason.Reason, issuer: String, order: () => Unit)
