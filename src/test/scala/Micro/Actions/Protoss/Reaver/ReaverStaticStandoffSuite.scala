package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverStaticStandoffSuite extends AnyFunSuite {

  test("out of static reach the Reaver holds its shot") {
    assert( ! ReaverStaticStandoff.shouldBackOff(distTiles = 7.5, ownRangeTiles = 8.0, foeRangeTiles = 7.0))
    assert( ! ReaverStaticStandoff.shouldBackOff(distTiles = 9.0, ownRangeTiles = 8.0, foeRangeTiles = 7.0))
  }

  test("inside static reach the Reaver backs off when it outranges the static") {
    assert(ReaverStaticStandoff.shouldBackOff(distTiles = 7.4, ownRangeTiles = 8.0, foeRangeTiles = 7.0))
    assert(ReaverStaticStandoff.shouldBackOff(distTiles = 0.0, ownRangeTiles = 8.0, foeRangeTiles = 7.0))
  }

  test("an outranging static is never backed off from") {
    assert( ! ReaverStaticStandoff.shouldBackOff(distTiles = 0.0, ownRangeTiles = 4.0, foeRangeTiles = 7.0))
  }

  test("the backoff lands exactly on our max range") {
    assert(ReaverStaticStandoff.backoffTiles(distTiles = 2.0, ownRangeTiles = 8.0) == 6.0)
    assert(ReaverStaticStandoff.backoffTiles(distTiles = 8.0, ownRangeTiles = 8.0) == 0.0)
    assert(ReaverStaticStandoff.backoffTiles(distTiles = 9.0, ownRangeTiles = 8.0) == 0.0)
  }

  test("a parked Reaver backs off a pressing static, nothing else") {
    val idle = ReaverSituation()
    assert( ! ReaverBackOffStatic.applies(idle))
    assert(ReaverBackOffStatic.applies(idle.copy(staticInFace = true)))
    assert( ! ReaverBackOffStatic.applies(idle.copy(staticInFace = true, loaded = true)))
    assert( ! ReaverBackOffStatic.applies(idle.copy(staticInFace = true, committed = true)))
  }
}
