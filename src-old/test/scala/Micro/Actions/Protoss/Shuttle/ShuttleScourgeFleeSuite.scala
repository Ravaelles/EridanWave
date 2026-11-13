package Micro.Actions.Protoss.Shuttle

import org.scalatest.funsuite.AnyFunSuite

class ShuttleScourgeFleeSuite extends AnyFunSuite {

  test("only a transport carrying Reavers runs from Scourge") {
    assert(ShuttleScourgeFlee.shouldRun(hasLoadedReaver = true, scourgeWithinTiles = true))
    assert( ! ShuttleScourgeFlee.shouldRun(hasLoadedReaver = false, scourgeWithinTiles = true))
    assert( ! ShuttleScourgeFlee.shouldRun(hasLoadedReaver = true, scourgeWithinTiles = false))
    assert( ! ShuttleScourgeFlee.shouldRun(hasLoadedReaver = false, scourgeWithinTiles = false))
  }
}
