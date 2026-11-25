package Micro.Actions.Combat.Maneuvering

import org.scalatest.funsuite.AnyFunSuite

class MineralLineHeatSuite extends AnyFunSuite {

  test("heat needs fresh damage and no visible enemy") {
    assert(MineralLineHeat.heating(minerDamagedRecently = true, visibleEnemyNear = false))
    assert( ! MineralLineHeat.heating(minerDamagedRecently = true, visibleEnemyNear = true))
    assert( ! MineralLineHeat.heating(minerDamagedRecently = false, visibleEnemyNear = false))
  }

  test("calm needs a full quiet window") {
    assert( ! MineralLineHeat.calm(MineralLineHeat.calmFrames))
    assert(MineralLineHeat.calm(MineralLineHeat.calmFrames + 1))
  }
}
