package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarHuntSuite extends AnyFunSuite {

  test("the hunt reaches forty ground tiles from the main") {
    assert(DarkTemplarHunt.inHuntRange(0.0))
    assert(DarkTemplarHunt.inHuntRange(40.0))
    assert( ! DarkTemplarHunt.inHuntRange(40.1))
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
