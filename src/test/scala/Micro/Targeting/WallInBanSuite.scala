package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class WallInBanSuite extends AnyFunSuite {

  test("five dragoons per sieged tank hold the wall, fewer ban it") {
    assert( ! WallInBan.outgunned(dragoons = 5, siegedTanks = 1))
    assert( ! WallInBan.outgunned(dragoons = 10, siegedTanks = 2))
    assert( ! WallInBan.outgunned(dragoons = 0, siegedTanks = 0))
    assert( ! WallInBan.outgunned(dragoons = 4, siegedTanks = 1))
    assert(WallInBan.outgunned(dragoons = 0, siegedTanks = 1))
    assert(WallInBan.outgunned(dragoons = 1, siegedTanks = 6))
  }

  test("a maxed line or a bank releases the ban") {
    assert(WallInBan.released(supplyUsed = 195, minerals = 0))
    assert(WallInBan.released(supplyUsed = 0, minerals = 1200))
    assert( ! WallInBan.released(supplyUsed = 194, minerals = 1199))
  }

  test("the ban latches past the ratio and releases on the bank") {
    def latch(outgunned: Boolean, supply: Int, minerals: Int): Boolean =
      WallInBan.banLatched(outgunned, supply, minerals)
    assert( ! latch(outgunned = false, supply = 30, minerals = 0))
    assert(latch(outgunned = true, supply = 30, minerals = 0))
    assert(latch(outgunned = false, supply = 100, minerals = 0))
    assert( ! latch(outgunned = false, supply = 195, minerals = 0))
    assert( ! latch(outgunned = false, supply = 0, minerals = 1200))
  }
}
