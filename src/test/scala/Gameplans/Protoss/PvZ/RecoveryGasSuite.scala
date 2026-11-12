package Gameplans.Protoss.PvZ

import org.scalatest.funsuite.AnyFunSuite

class RecoveryGasSuite extends AnyFunSuite {

  test("fourteen produced warriors gas up") {
    assert(RecoveryGas.shouldGas(14))
    assert(RecoveryGas.shouldGas(60))
    assert( ! RecoveryGas.shouldGas(13))
    assert( ! RecoveryGas.shouldGas(0))
  }
}
