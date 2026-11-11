package Tactic.Squads.Decisions

import RegroupLatch.{Engaged, Regrouping, Stance}
import Utilities.?

/**
 * Temporal confirmation for stance votes (Stardust activity latch companion).
 * The sim re-judges every estimation cycle from live positions, so near-even
 * scores flicker and the raw latch proposal flaps Engaged/Regrouping every
 * few frames - swinging destinations, formations and targets for the whole
 * squad. This filter confirms retreat proposals over consecutive frames
 * before adopting them; re-engage proposals pass through instantly (the
 * worst case re-flips and re-confirms a retreat). Alternating noise never
 * accumulates a streak, so it never flips the stance.
 *
 * Stateful per squad (one instance per consumer); the confirm function is
 * total and deterministic. Knob is a public var, KiteAway style, for tuning.
 */
final class StanceVoteFilter(var confirmFrames: Int = 24) {

  private var retreatStreak: Int = 0

  def confirm(adopted: Stance, proposed: Stance): Stance = (adopted, proposed) match {
    case (_, Engaged)        => retreatStreak = 0; Engaged
    case (Engaged, _)        => retreatStreak += 1; ?(retreatStreak >= confirmFrames, Regrouping, Engaged)
    case (Regrouping, _)     => retreatStreak = 0; Regrouping
  }
}
