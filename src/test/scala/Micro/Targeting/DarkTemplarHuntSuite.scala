package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarHuntSuite extends AnyFunSuite {

  test("a small army hunts to fifty ground tiles from the main") {
    assert(DarkTemplarHunt.huntRangeTiles(0) == 50.0)
    assert(DarkTemplarHunt.huntRangeTiles(29) == 50.0)
    assert(DarkTemplarHunt.inHuntRange(50.0, 29))
    assert( ! DarkTemplarHunt.inHuntRange(50.1, 29))
  }

  test("a large army holds the old forty") {
    assert(DarkTemplarHunt.huntRangeTiles(30) == 40.0)
    assert(DarkTemplarHunt.huntRangeTiles(200) == 40.0)
    assert(DarkTemplarHunt.inHuntRange(40.0, 30))
    assert( ! DarkTemplarHunt.inHuntRange(40.1, 30))
  }

  test("nearest to the main wins, never nearest to me") {
    assert(DarkTemplarHunt.pick(Seq.empty).isEmpty)
    assert(DarkTemplarHunt.pick(Seq(DarkTemplarHunt.Mark(30.0))).contains(0))
    assert(DarkTemplarHunt.pick(Seq(
      DarkTemplarHunt.Mark(30.0),
      DarkTemplarHunt.Mark(10.0),
      DarkTemplarHunt.Mark(20.0))).contains(1))
  }
}
