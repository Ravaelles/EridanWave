package Utilities

import org.scalatest.funsuite.AnyFunSuite

class LatchSuite extends AnyFunSuite {

  test("fires sharply, holds inside the band, releases past it") {
    val latch = new Latch
    assert(latch(1, fires = true, releases = false))
    assert(latch(1, fires = false, releases = false))
    assert( ! latch(1, fires = false, releases = true))
    assert( ! latch(1, fires = false, releases = false))
  }

  test("the first frame inside the band takes the caller's initial side") {
    val latch = new Latch
    assert(latch(7, fires = false, releases = false, initial = true))
    assert(latch(7, fires = false, releases = false))
    assert( ! latch(8, fires = false, releases = false, initial = false))
  }

  test("keys hold their own side") {
    val latch = new Latch
    assert(latch(1, fires = true, releases = false))
    assert( ! latch(2, fires = false, releases = false))
    assert(latch(1, fires = false, releases = false))
    assert( ! latch(2, fires = false, releases = false))
  }

  test("a game restart clears every registered latch, so ids never inherit") {
    val latch = new Latch
    assert(latch(42, fires = true, releases = false))
    Latches.resetAll()
    assert( ! latch(42, fires = false, releases = false))
  }
}
