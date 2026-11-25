package Micro.Actions.Combat.Decisioning

import Tactic.Squads.Decisions.MotionVerdict

/**
 * Engage advance for postureless squads (pure modes, no game access):
 * the duel reads the latched feet-owner instead of the raw consensus,
 * so a held duel rides through the sim seesaw instead of flapping the
 * march and the duel every other frame (VolleyHold/March till death).
 * Withdraw never duels on the way out; Dress and the open march keep
 * the old consensus gate untouched.
 */
object EngageAdvance {

  def posturelessAdvance(held: MotionVerdict.Mode, fightConsensus: Boolean, marchHome: Boolean): Boolean =
    held match {
      case MotionVerdict.Duel     => ! marchHome
      case MotionVerdict.Withdraw => false
      case _                      => fightConsensus && ! marchHome
    }
}
