package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class StormStandSuite extends AnyFunSuite {

  private def holds(
    isTemplar: Boolean = true,
    enemyIsZerg: Boolean = true,
    energyReady: Boolean = true,
    nearestFoeTiles: Double = 10.0,
    fightConsensus: Boolean = false,
    siegeThreat: Boolean = false): Boolean =
    StormStand.shouldHold(isTemplar, enemyIsZerg, energyReady, nearestFoeTiles, fightConsensus, siegeThreat)

  test("a loaded Templar holds the line inside the approach band") {
    assert(holds())
    assert(holds(nearestFoeTiles = StormStand.approachTiles))
  }

  test("a fighting squad re-arms the walk anywhere") {
    assert(holds(nearestFoeTiles = 30.0, fightConsensus = true))
  }

  test("unsieged Terran is allowed danger: the walk holds, the cast vetoes the shells") {
    assert(holds(enemyIsZerg = false, siegeThreat = false))
    assert( ! holds(enemyIsZerg = false, siegeThreat = true))
  }

  test("dry, unarmed or far hulls retreat as before") {
    assert( ! holds(energyReady = false))
    assert( ! holds(isTemplar = false))
    assert( ! holds(nearestFoeTiles = StormStand.approachTiles + 0.1))
  }

  test("siege never stops a Zerg-race stand, only the walk-in") {
    assert(holds(enemyIsZerg = true, nearestFoeTiles = 30.0, fightConsensus = true, siegeThreat = true))
  }
}
