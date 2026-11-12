package Micro.Actions.Protoss.DarkTemplar

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarBerthSuite extends AnyFunSuite {

  test("a fresh Templar takes no extra berth") {
    assert(DarkTemplarBerth.extraTiles(0.0) == 0.0)
  }

  test("the berth grows linearly to eight tiles when dying") {
    assert(DarkTemplarBerth.extraTiles(0.5) == 4.0)
    assert(DarkTemplarBerth.extraTiles(1.0) == 8.0)
  }

  test("wounds outside 0-1 clamp to the band") {
    assert(DarkTemplarBerth.extraTiles(-1.0) == 0.0)
    assert(DarkTemplarBerth.extraTiles(2.0) == 8.0)
  }
}
