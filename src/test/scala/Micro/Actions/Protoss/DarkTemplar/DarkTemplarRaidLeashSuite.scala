package Micro.Actions.Protoss.DarkTemplar

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarRaidLeashSuite extends AnyFunSuite {

  test("workers and loners inside sixty hold unless already dueling") {
    assert(DarkTemplarRaidLeash.holdRaid(isWorker = true, loneWarrior = false, distToEnemyMainTiles = 60.0, distToTargetTiles = 1.11))
    assert(DarkTemplarRaidLeash.holdRaid(isWorker = false, loneWarrior = true, distToEnemyMainTiles = 0.0, distToTargetTiles = 50.0))
    assert( ! DarkTemplarRaidLeash.holdRaid(isWorker = true, loneWarrior = false, distToEnemyMainTiles = 60.0, distToTargetTiles = 1.1))
    assert( ! DarkTemplarRaidLeash.holdRaid(isWorker = true, loneWarrior = false, distToEnemyMainTiles = 60.0, distToTargetTiles = 0.5))
  }

  test("packs, the far truss and open ground keep hunting") {
    assert( ! DarkTemplarRaidLeash.holdRaid(isWorker = false, loneWarrior = false, distToEnemyMainTiles = 0.0, distToTargetTiles = 50.0))
    assert( ! DarkTemplarRaidLeash.holdRaid(isWorker = true, loneWarrior = false, distToEnemyMainTiles = 60.01, distToTargetTiles = 50.0))
    assert( ! DarkTemplarRaidLeash.holdRaid(isWorker = true, loneWarrior = false, distToEnemyMainTiles = 200.0, distToTargetTiles = 50.0))
  }
}
