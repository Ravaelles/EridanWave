package Tactic.Squads.Decisions

/**
 * Retreat discipline (Stardust regroup parity, cluster scale): once the
 * cluster breaks off, it stays broken off. A withdrawal that re-engages
 * the moment a sim flickers back to optimistic feeds the line piecemeal -
 * the retreat/attack/retreat saw is exactly the flap the posture refactor
 * exists to kill.
 *
 * Two mechanisms, both pure numbers for the suite:
 *
 * 1. Minimum withdrawal length: the posture may not leave Withdraw for
 *    this many frames after entering it, no matter what the forecast says.
 * 2. Decaying advantage penalty: while the wound is fresh the local
 *    advantage reads weaker than the sim claims (1s: -40%, 2s: -30%,
 *    3s: -20%, 4-6s: -15%), so borderline forecasts cannot re-trigger a
 *    fight the cluster just refused. The penalty multiplies the sighed
 *    strength ratio, never the raw counts.
 */
object RetreatDiscipline {

  /** Withdrawal floor, in frames: nothing re-engages sooner than this. */
  val minWithdrawFrames: Int = 48

  /**
   * Advantage multiplier by frames since the withdrawal started: fresh
   * wounds read weakest, old ones read whole again.
   */
  def advantageMultiplier(framesSinceWithdraw: Int): Double =
    if (framesSinceWithdraw < 24) 0.6
    else if (framesSinceWithdraw < 48) 0.7
    else if (framesSinceWithdraw < 72) 0.8
    else if (framesSinceWithdraw <= 144) 0.85
    else 1.0

  /**
   * Withdrawal hold (pure rule for the posture latch): stay withdrawn
   * while the floor has not elapsed; after that the penalized advantage
   * decides, exactly like any other frame.
   */
  def shouldHoldWithdraw(framesSinceWithdraw: Int): Boolean =
    framesSinceWithdraw < minWithdrawFrames
}
