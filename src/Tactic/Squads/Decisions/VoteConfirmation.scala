package Tactic.Squads.Decisions

import Utilities.?

/**
 * Temporal confirmation for a squad vote: near-even votes flicker every few
 * frames, and each flip swings destinations, formations and targets for the
 * whole squad. This filter adopts the slow (risky) proposal only after it
 * holds for consecutive frames; proposals equal to the fast value pass
 * through instantly, because the worst case is confirming the slow value
 * again. Alternating noise never accumulates a streak, so it never flips.
 *
 * Stateful per squad (one instance per consumer); confirm is total and
 * deterministic. Knob is a public var, KiteAway style, for tuning.
 *
 * @param fastPath the value adopted instantly (re-engage, rejoin, true)
 * @param slowPath the value that needs confirming (retreat, leave, false)
 */
final class VoteConfirmation[A](fastPath: A, slowPath: A, var confirmFrames: Int = 24) {

  private var streak: Int = 0

  def confirm(adopted: A, proposed: A): A = {
    if (proposed == fastPath) {
      streak = 0
      fastPath
    } else if (adopted == fastPath) {
      streak += 1
      ?(streak >= confirmFrames, slowPath, fastPath)
    } else {
      streak = 0
      slowPath
    }
  }
}
