package Micro.Actions

/**
 * Verdict of a unit-controlling dispatch (one per frame per dispatch):
 * either this behavior owns the frame or it declines and the next one
 * runs. The type - never a bare boolean - is the contract: a dispatch
 * that controls a unit answers in UnitControl, so a new behavior that
 * forgets its verdict does not compile, and every claimed frame carries
 * its owner's name through the chain credit beside it. No third state:
 * ownership is binary per frame; soft claims compose outside the frame.
 */
sealed abstract class UnitControl(val claimed: Boolean)

object UnitControl {

  /** This behavior owns the frame: it issued orders, held, or shot. */
  case object Claimed extends UnitControl(true)

  /** This behavior did nothing: the next claimant runs instead. */
  case object Declined extends UnitControl(false)

  /** Lift a legacy boolean verdict into the contract. */
  def claimedIf(ownsFrame: Boolean): UnitControl =
    if (ownsFrame) Claimed else Declined
}
