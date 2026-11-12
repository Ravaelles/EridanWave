package Macro.Actions

import org.scalatest.funsuite.AnyFunSuite

class PumpWorkersSuite extends AnyFunSuite {

  test("small worker counts pass the ceiling untouched") {
    assert(PumpWorkers.capTotal(0) == 0)
    assert(PumpWorkers.capTotal(21) == 21)
    assert(PumpWorkers.capTotal(63) == 63)
  }

  test("nothing produces past 63 workers") {
    assert(PumpWorkers.capTotal(64) == 63)
    assert(PumpWorkers.capTotal(75) == 63)
    assert(PumpWorkers.capTotal(400) == 63)
  }
}
