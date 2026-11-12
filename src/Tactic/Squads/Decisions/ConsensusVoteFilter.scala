package Tactic.Squads.Decisions

/**
 * Temporal confirmation for the fight-consensus vote (StanceVoteFilter
 * companion for the squad destination). Near-even votes flicker every few
 * frames - contact-band flicker at the 1.5-tile band, sim judgments
 * appearing and vanishing as battles re-cluster - and each flip swings
 * every terminus and target between the formation and home, so a rear
 * unit walks, stops, and walks again instead of joining the army. This
 * filter confirms leaving proposals over consecutive frames before
 * adopting them; rejoin proposals pass through instantly (the worst case
 * re-flips and re-confirms a retreat). Alternating noise never accumulates
 * a streak, so it never flips the consensus.
 *
 * Stateful per squad (one instance per consumer); the confirm function is
 * total and deterministic. Updated exactly once per frame by Squad, never
 * from the fightConsensus getter itself - the getter runs many times per
 * frame and would burn the streak. Knob is a public var, KiteAway style,
 * for tuning.
 */
final class ConsensusVoteFilter(var confirmFrames: Int = 24) {

  private var leaveStreak: Int = 0

  def confirm(adopted: Boolean, proposed: Boolean): Boolean = (adopted, proposed) match {
    case (_, true)  => leaveStreak = 0; true
    case (true, _)  => leaveStreak += 1; leaveStreak >= confirmFrames
    case (false, _) => leaveStreak = 0; false
  }
}
