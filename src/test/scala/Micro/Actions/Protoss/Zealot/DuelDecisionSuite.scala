package Micro.Actions.Protoss.Zealot

import org.scalatest.funsuite.AnyFunSuite

class DuelDecisionSuite extends AnyFunSuite {

  import DuelDecision._

  private def facts(
      outnumbered : Boolean = false,
      dying       : Boolean = false,
      duelling    : Boolean = false,
      outnumbering: Boolean = false): DuelFacts =
    DuelFacts(outnumbered, dying, duelling, outnumbering)

  test("no rule firing keeps previous orders") {
    assert(decide(facts()) == Undecided)
  }

  test("each rule fires alone") {
    assert(decide(facts(outnumbered = true)) == Outnumbered)
    assert(decide(facts(dying = true)) == Dying)
    assert(decide(facts(duelling = true)) == Duelling)
    assert(decide(facts(outnumbering = true)) == Outnumbering)
  }

  test("priority follows the historical order") {
    assert(decide(facts(outnumbered = true, dying = true, duelling = true, outnumbering = true)) == Outnumbered)
    assert(decide(facts(dying = true, duelling = true, outnumbering = true)) == Dying)
    assert(decide(facts(duelling = true, outnumbering = true)) == Duelling)
  }
}
