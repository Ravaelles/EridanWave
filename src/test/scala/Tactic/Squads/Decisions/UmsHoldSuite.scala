package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class UmsHoldSuite extends AnyFunSuite {

  test("pins the hold bands") {
    assert(UmsHold.strongScore == 250.0)
    assert(UmsHold.evenScore == 100.0)
    assert(UmsHold.fastStrideTiles == 0.75)
    assert(UmsHold.slowStrideTiles == 0.15)
    assert(UmsHold.settleFraction == 0.8)
    assert(UmsHold.pauseFrames == 2)
  }

  test("even or losing holds the short stride, dominant takes the long one") {
    assert(UmsHold.strideTiles(0.0) == 0.15)
    assert(UmsHold.strideTiles(100.0) == 0.15)
    assert(UmsHold.strideTiles(250.0) == 0.75)
    assert(UmsHold.strideTiles(500.0) == 0.75)
  }

  test("the stride grows with the score between even and dominant") {
    assert(math.abs(UmsHold.strideTiles(175.0) - 0.45) < 1e-9)
    assert(UmsHold.strideTiles(140.0) < UmsHold.strideTiles(175.0))
    assert(UmsHold.strideTiles(210.0) > UmsHold.strideTiles(175.0))
  }

  test("four in five hulls standing lets the line step") {
    assert(UmsHold.settled(stationary = 8, total = 10))
    assert(UmsHold.settled(stationary = 10, total = 10))
    assert( ! UmsHold.settled(stationary = 7, total = 10))
    assert( ! UmsHold.settled(stationary = 0, total = 10))
  }

  test("an empty line never waits for itself") {
    assert(UmsHold.settled(stationary = 0, total = 0))
  }

  test("strides pause between steps") {
    UmsHold.reset()
    assert(UmsHold.strideNow(1, frame = 0))
    assert( ! UmsHold.strideNow(1, frame = 1))
    assert( ! UmsHold.strideNow(1, frame = 2))
    assert(UmsHold.strideNow(1, frame = 3))
    assert(UmsHold.strideNow(2, frame = 3))
  }
}
