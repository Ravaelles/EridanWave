package Micro.Actions.Basic

import org.scalatest.funsuite.AnyFunSuite

class WorkerCombatSupportSuite extends AnyFunSuite {

  test("a fighting warrior anchors the miner") {
    assert(WorkerCombatSupport.mayFight(1))
    assert(WorkerCombatSupport.mayFight(3))
  }

  test("no fighting warrior means no fight") {
    assert( ! WorkerCombatSupport.mayFight(0))
  }

  test("support radius is ten tiles") {
    assert(WorkerCombatSupport.supportTiles == 10)
    assert(WorkerCombatSupport.supportPixels == 320.0)
  }
}
