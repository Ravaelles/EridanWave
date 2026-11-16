package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class ReengageLatchSuite extends AnyFunSuite {

  private def holds(
    wasRetreating: Boolean = true,
    entangledTiles: Double = 0.0,
    verdictFight: Boolean = true,
    confidence11: Double = 0.0): Boolean =
    ReengageLatch.shouldHoldRetreat(wasRetreating, entangledTiles, verdictFight, confidence11)

  test("a fresh unit never holds a retreat it never started") {
    assert( ! holds(wasRetreating = false))
  }

  test("a hot retreat holds through a fight verdict") {
    assert(holds(entangledTiles = 0.0))
    assert(holds(entangledTiles = -ReengageLatch.clearTiles + 0.1))
  }

  test("clear with margin and a fight verdict releases") {
    assert( ! holds(entangledTiles = -ReengageLatch.clearTiles, verdictFight = true))
  }

  test("a lost verdict holds even past the line") {
    assert(holds(entangledTiles = -ReengageLatch.clearTiles - 5.0, verdictFight = false))
  }

  test("overmatch turns inside contact") {
    assert( ! holds(entangledTiles = 0.0, confidence11 = ReengageLatch.confidentToReturn + 0.01))
    assert(holds(entangledTiles = 0.0, confidence11 = ReengageLatch.confidentToReturn))
  }
}
