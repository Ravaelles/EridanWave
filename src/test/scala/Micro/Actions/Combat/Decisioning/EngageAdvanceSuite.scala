package Micro.Actions.Combat.Decisioning

import Tactic.Squads.Decisions.MotionVerdict
import org.scalatest.funsuite.AnyFunSuite

class EngageAdvanceSuite extends AnyFunSuite {

  test("a held duel advances through a dead consensus, never marching home") {
    assert(EngageAdvance.posturelessAdvance(MotionVerdict.Duel, fightConsensus = false, marchHome = false))
    assert(EngageAdvance.posturelessAdvance(MotionVerdict.Duel, fightConsensus = true, marchHome = false))
    assert( ! EngageAdvance.posturelessAdvance(MotionVerdict.Duel, fightConsensus = true, marchHome = true))
  }

  test("a held retreat never duels on the way out") {
    assert( ! EngageAdvance.posturelessAdvance(MotionVerdict.Withdraw, fightConsensus = true, marchHome = false))
    assert( ! EngageAdvance.posturelessAdvance(MotionVerdict.Withdraw, fightConsensus = false, marchHome = false))
  }

  test("dress and the open march keep the old consensus gate") {
    assert(EngageAdvance.posturelessAdvance(MotionVerdict.Dress, fightConsensus = true, marchHome = false))
    assert( ! EngageAdvance.posturelessAdvance(MotionVerdict.Dress, fightConsensus = false, marchHome = false))
    assert(EngageAdvance.posturelessAdvance(MotionVerdict.Advance, fightConsensus = true, marchHome = false))
    assert( ! EngageAdvance.posturelessAdvance(MotionVerdict.Advance, fightConsensus = false, marchHome = false))
    assert( ! EngageAdvance.posturelessAdvance(MotionVerdict.Advance, fightConsensus = true, marchHome = true))
  }
}
