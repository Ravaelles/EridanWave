package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class WorkerSpreadSuite extends AnyFunSuite {

  test("no workers means the first slot") {
    assert(WorkerSpread.pickSlot(attackerId = 7, workers = 0) == 0)
  }

  test("one worker is everybody's worker") {
    assert(WorkerSpread.pickSlot(attackerId = 1, workers = 1) == 0)
    assert(WorkerSpread.pickSlot(attackerId = 99, workers = 1) == 0)
  }

  test("attackers deal across the line by id") {
    assert(WorkerSpread.pickSlot(attackerId = 10, workers = 3) == 1)
    assert(WorkerSpread.pickSlot(attackerId = 11, workers = 3) == 2)
    assert(WorkerSpread.pickSlot(attackerId = 12, workers = 3) == 0)
  }

  test("the draw caps at three") {
    assert(WorkerSpread.pickSlot(attackerId = 7, workers = 10) == 1)
    assert(WorkerSpread.pickSlot(attackerId = 8, workers = 2) == 0)
    assert(WorkerSpread.pickSlot(attackerId = 9, workers = 2) == 1)
  }
}
