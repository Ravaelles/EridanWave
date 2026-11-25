package Micro.Agency

/**
 * Per-frame issued-order fuse (Stardust DUPLICATE ORDER, phase 0): exactly
 * one BWAPI issuance owns a unit per frame. The first issuer in a frame
 * claims the fuse; any second issuance in the same frame is a duplicate and
 * must not reach the engine. The fuse is frame-bound, never latched: a new
 * frame releases it with no reset call, so no reset ordering can wedge it.
 * Pure core (no game reads, the caller supplies the frame), pinned beside
 * the callers it guards; per-unit storage and ledger writes stay in
 * CommanderCore.
 */
object IssuedOrderFuse {

  /** The last BWAPI issuance recorded on one unit: frame plus first door. */
  case class Claim(frame: Int, issuer: OrderIssuer.Issuer)

  /**
   * Attempt to record an issuance. Returns the stored claim and whether this
   * issuance is a duplicate: the second or later issuance inside one frame,
   * from any issuer - a re-issue from the same site is still a second order.
   * Duplicates keep the first claim, so the ledger names first-vs-second.
   */
  def tryClaim(last: Option[Claim], frame: Int, issuer: OrderIssuer.Issuer): (Option[Claim], Boolean) =
    last match {
      case Some(Claim(`frame`, _)) => (last, true)
      case _                       => (Some(Claim(frame, issuer)), false)
    }

  /**
   * Stable ledger key for a duplicate: first-vs-second issuer at the unit
   * class, greppable beside the exception keys. Pure so the suite pins the
   * format the end-of-game summary will carry.
   */
  def duplicateKey(first: OrderIssuer.Issuer, second: OrderIssuer.Issuer, className: String): String =
    f"duplicateOrder.${first.key}%sVs${second.key}%s.at.$className%s"
}
