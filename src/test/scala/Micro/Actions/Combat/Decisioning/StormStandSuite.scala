package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class StormStandSuite extends AnyFunSuite {

  private def holds(
    isTemplar: Boolean = true,
    enemyIsZerg: Boolean = true,
    energyReady: Boolean = true,
    nearestFoeTiles: Double = 10.0,
    fightConsensus: Boolean = false): Boolean =
    StormStand.shouldHold(isTemplar, enemyIsZerg, energyReady, nearestFoeTiles, fightConsensus)

  test("a loaded Templar holds the line inside the approach band") {
    assert(holds())
    assert(holds(nearestFoeTiles = StormStand.approachTiles))
  }

  test("a fighting squad re-arms the walk anywhere") {
    assert(holds(nearestFoeTiles = 30.0, fightConsensus = true))
  }

  test("dry, wrong-race or far hulls retreat as before") {
    assert( ! holds(energyReady = false))
    assert( ! holds(enemyIsZerg = false))
    assert( ! holds(isTemplar = false))
    assert( ! holds(nearestFoeTiles = StormStand.approachTiles + 0.1))
  }
}
