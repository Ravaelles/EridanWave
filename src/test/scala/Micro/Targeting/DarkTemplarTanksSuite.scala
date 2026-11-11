package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarTanksSuite extends AnyFunSuite {

  test("hittable boundary sits at 1.1 tiles") {
    assert(DarkTemplarTanks.hittable(1.1))
    assert( ! DarkTemplarTanks.hittable(1.11))
    assert( ! DarkTemplarTanks.hittable(6.0))
  }

  test("sieged tanks range 6 tiles") {
    assert(DarkTemplarTanks.siegedNear(6.0))
    assert( ! DarkTemplarTanks.siegedNear(6.01))
  }

  test("unsieged tanks range 6 tiles") {
    assert(DarkTemplarTanks.unsiegedNear(6.0))
    assert( ! DarkTemplarTanks.unsiegedNear(6.01))
  }

  test("reach outranks tank range") {
    assert(DarkTemplarTanks.hittableTiles < DarkTemplarTanks.siegedTankTiles)
    assert(DarkTemplarTanks.siegedTankTiles == DarkTemplarTanks.unsiegedTankTiles)
  }
}
