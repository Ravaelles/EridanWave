package Tactic.Squads.Decisions

/**
 * One verdict per fighting cluster per frame (no game access): the whole
 * cluster shares a posture instead of each unit asking its own sim. Per-unit
 * sims survive as inputs (fighting fraction, penalized advantage) but never
 * as verdicts - that is what ends the half-flees-half-fights split. The
 * hysteresis is load-bearing: leaving Withdraw takes the floor plus sim
 * agreement plus parity, and the Fight edge carries a deadband, so neither
 * line flaps on its own boundary.
 */
object ClusterPosture {

  sealed trait Posture

  /** Close and kill; roles split the spearhead, the flanks, the support. */
  case object Fight extends Posture

  /** Hold formation and let the enemy come; melee still closes. */
  case object HoldLine extends Posture

  /** The whole cluster leaves; re-entry takes the floor plus agreement. */
  case object Withdraw extends Posture

  /** No battle yet or reforming; march dressed, never pose. */
  case object Regroup extends Posture

  /** Duel permission: Fight and HoldLine duel, Withdraw and Regroup
    * walk the march. One predicate so the engage gate and the technique
    * dispatch never disagree on what the posture allows. */
  def advances(posture: Posture): Boolean = posture == Fight || posture == HoldLine

  /** Penalized advantage below parity argues for leaving. */
  val withdrawBelowAdvantage: Double = 1.0

  /** Penalized advantage clearly superior argues for closing. */
  val fightAboveAdvantage: Double = 1.5

  /** Leaving Fight takes a clear drop, not the edge itself. */
  val fightHoldDeadband: Double = 0.2

  /** Sim agreement, asymmetric: flee below, rejoin above. */
  val startWithdrawBelow: Double = 0.35

  /** Sim agreement, asymmetric: flee below, rejoin above. */
  val stopWithdrawAbove: Double = 0.65

  /** Without this much of the squad judged, no forecast flips anything. */
  val minimumCoverage: Double = 0.5

  /**
   * Next posture (pure numbers for the suite): the caller penalizes the
   * advantage through RetreatDiscipline first, so a fresh wound reads
   * weaker than the sim claims without this kernel knowing about frames.
   * Baseless maps never withdraw, but they may hold: with nowhere to
   * regroup to, contact reads the same band as everywhere else - an
   * even or losing line holds instead of charging into a superior one.
   * Calm still regroups: with no contact there is nothing to hold
   * against, so the line dresses out instead of posing.
   * A forbidden retreat (RetreatAllowed) skips everything and reads the
   * band straight away: no floor to sit, no agreement to wait for.
   */
  def next(
    current: Posture,
    fightingFraction: Double,
    coverageFraction: Double,
    advantageRatio: Double,
    inContact: Boolean,
    framesSinceWithdraw: Int,
    canWithdraw: Boolean = true,
    retreatAllowed: Boolean = true): Posture = {
    if ( ! retreatAllowed) return band(current, advantageRatio, inContact)

    if (coverageFraction < minimumCoverage) return current

    if ( ! canWithdraw) return if (inContact) band(current, advantageRatio, inContact) else Regroup

    if (current == Withdraw) {
      if (RetreatDiscipline.shouldHoldWithdraw(framesSinceWithdraw)) return Withdraw

      if (fightingFraction > stopWithdrawAbove && advantageRatio >= withdrawBelowAdvantage) return band(current, advantageRatio, inContact)

      return Withdraw
    }

    if (fightingFraction < startWithdrawBelow) return Withdraw

    band(current, advantageRatio, inContact)
  }

  private def band(current: Posture, advantage: Double, inContact: Boolean): Posture = {
    if (advantage >= fightAboveAdvantage) return Fight

    if (current == Fight && advantage >= fightAboveAdvantage - fightHoldDeadband) return Fight

    if (inContact) HoldLine else Regroup
  }
}
