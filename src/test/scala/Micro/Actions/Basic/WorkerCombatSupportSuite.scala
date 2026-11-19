package Micro.Actions.Basic

import org.scalatest.funsuite.AnyFunSuite

class WorkerCombatSupportSuite extends AnyFunSuite {

  test("a fighting warrior anchors the miner") {
    assert(WorkerCombatSupport.mayFight(1, 45))
    assert(WorkerCombatSupport.mayFight(3, 60))
  }

  test("no fighting warrior means no fight") {
    assert( ! WorkerCombatSupport.mayFight(0, 45))
  }

  test("a dying worker rotates home instead of trading the line") {
    assert( ! WorkerCombatSupport.mayFight(2, 20))
    assert( ! WorkerCombatSupport.mayFight(2, 5))
    assert(WorkerCombatSupport.mayFight(2, 21))
  }

  test("support radius is ten tiles") {
    assert(WorkerCombatSupport.supportTiles == 10)
    assert(WorkerCombatSupport.supportPixels == 320.0)
  }
}
