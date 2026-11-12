package Micro.Actions.Combat.Maneuvering

import org.scalatest.funsuite.AnyFunSuite

class WorkerThreatFleeSuite extends AnyFunSuite {

  test("three mutalisks make the pack") {
    assert(WorkerThreatFlee.mutaPack(3))
    assert(WorkerThreatFlee.mutaPack(9))
    assert( ! WorkerThreatFlee.mutaPack(2))
  }

  test("shelling counts inside tank range plus margin") {
    assert(WorkerThreatFlee.tankShelling(distTiles = 13.0, tankRangeTiles = 12.0))
    assert(WorkerThreatFlee.tankShelling(distTiles = 5.0, tankRangeTiles = 12.0))
    assert( ! WorkerThreatFlee.tankShelling(distTiles = 14.01, tankRangeTiles = 12.0))
    assert( ! WorkerThreatFlee.tankShelling(distTiles = 20.0, tankRangeTiles = 12.0))
  }
}
