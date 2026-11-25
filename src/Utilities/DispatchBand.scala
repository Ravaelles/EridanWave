package Utilities

/**
 * Dispatch band (one home for the arbitration vocabulary): every stage of
 * every first-claims chain declares which band it fights in. Bands
 * document squadIntent - survival before duels, duels before leftovers - but
 * the ORDER is the contract: each chain exposes its stage list as data
 * and an order suite pins the exact sequence. Reorder by moving a line,
 * disable by commenting one out. New bands arrive here, never as
 * one-off strings per chain.
 */
sealed trait DispatchBand

object DispatchBand {

  /** Survival and unconditional kills: unseen killers, shells, the
   * always-attack ladder, the retreat itself. Ends the frame. */
  case object High extends DispatchBand

  /** The duel itself: stands, brawls, abuses, kites, rearguard cover,
   * and the technique legs that execute inside a fight. */
  case object Medium extends DispatchBand

  /** Whatever the squad phase left over: march destinations, slots,
   * rally, repositioning. Runs only when nothing above claims. */
  case object Low extends DispatchBand
}
