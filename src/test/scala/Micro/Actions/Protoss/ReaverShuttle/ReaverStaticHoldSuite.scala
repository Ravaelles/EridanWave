package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverStaticHoldSuite extends AnyFunSuite {

  test("the launch band ends at seven and a quarter tiles") {
    assert(ReaverStaticHold.inHold(0.0))
    assert(ReaverStaticHold.inHold(7.25))
    assert( ! ReaverStaticHold.inHold(7.26))
  }

  test("the band releases half a tile past the entry") {
    assert( ! ReaverStaticHold.released(7.25))
    assert( ! ReaverStaticHold.released(7.75))
    assert(ReaverStaticHold.released(7.76))
  }

  test("the latch holds inside the band and drops past it") {
    val hull = 700101
    assert(ReaverStaticHold.holdLatched(hull, 7.0))
    assert(ReaverStaticHold.holdLatched(hull, 7.5))
    assert( ! ReaverStaticHold.holdLatched(hull, 8.0))
    assert( ! ReaverStaticHold.holdLatched(hull, 7.5))
  }

  test("one hold in eight frames, staggered per hull") {
    assert(ReaverStaticHold.holdFrame(0, 0))
    assert( ! ReaverStaticHold.holdFrame(1, 0))
    assert(ReaverStaticHold.holdFrame(8, 0))
    assert( ! ReaverStaticHold.holdFrame(8, 1))
    assert(ReaverStaticHold.holdFrame(7, 1))
  }
}
