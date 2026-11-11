package Tactic.Missions.evaluation

import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

class KillCreditsSuite extends AnyFunSuite {

  test("a kill books its bounty on an unknown killer") {
    KillCredits.reset()
    val total = KillCredits.noteKill(killerId = 7, Zerg.Zergling)
    assert(total == UnitBounty.weightedOf(Zerg.Zergling))
    assert(KillCredits.balanceOf(7) == total)
  }

  test("bounties accumulate on one killer") {
    KillCredits.reset()
    KillCredits.noteKill(killerId = 7, Zerg.Zergling)
    val total = KillCredits.noteKill(killerId = 7, Zerg.Zergling)
    assert(total == 2 * UnitBounty.weightedOf(Zerg.Zergling))
  }

  test("unknown units and classes read zero") {
    KillCredits.reset()
    assert(KillCredits.balanceOf(4242) == 0.0)
    assert(KillCredits.classAverageOf(Protoss.DarkTemplar) == 0.0)
    assert(KillCredits.recentAverageOf(Protoss.DarkTemplar, 2) == 0.0)
  }

  test("reset clears balances and averages") {
    KillCredits.noteKill(killerId = 7, Zerg.Zergling)
    KillCredits.reset()
    assert(KillCredits.balanceOf(7) == 0.0)
  }

  test("short names strip the race prefix") {
    assert(KillCredits.shortName(Protoss.DarkTemplar) == "Dark Templar")
    assert(KillCredits.shortName(Zerg.Zergling) == "Zergling")
  }
}
