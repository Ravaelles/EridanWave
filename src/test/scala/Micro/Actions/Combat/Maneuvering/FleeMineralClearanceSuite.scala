package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class FleeMineralClearanceSuite extends AnyFunSuite {

  test("only large ground units keep clear") {
    assert(FleeMineralClearance.applies(isLarge = true, flying = false))
    assert( ! FleeMineralClearance.applies(isLarge = false, flying = false))
    assert( ! FleeMineralClearance.applies(isLarge = true, flying = true))
  }

  test("minerals keep 1.2 tiles, geysers 3.1") {
    assert(FleeMineralClearance.mineralClearTiles == 1.2)
    assert(FleeMineralClearance.geyserClearTiles == 3.1)
  }

  test("mineral ring boundary") {
    assert( ! FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq(Pixel(38, 0)), Seq.empty))
    assert(FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq(Pixel(39, 0)), Seq.empty))
  }

  test("geyser ring boundary") {
    assert( ! FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq.empty, Seq(Pixel(99, 0))))
    assert(FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq.empty, Seq(Pixel(100, 0))))
  }

  test("minerals and geysers veto independently") {
    val nearMineral = Pixel(0, 30)
    val nearGeyser = Pixel(30, 0)
    val far = Pixel(500, 500)
    assert( ! FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq(nearMineral, far), Seq(far)))
    assert( ! FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq(far), Seq(far, nearGeyser)))
    assert(FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq(far), Seq(far)))
    assert(FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq.empty, Seq.empty))
  }

  test("the mineral push lands on the away-ray and settles") {
    val pushed = FleeMineralClearance.pushClear(Pixel(10, 0), Seq(Pixel(0, 0)), Seq.empty)
    assert(pushed == Pixel(38, 0))
    assert(FleeMineralClearance.pushClear(pushed, Seq(Pixel(0, 0)), Seq.empty) == pushed)
  }

  test("the geyser push strides past it, onto the away-ray") {
    val pushed = FleeMineralClearance.pushClear(Pixel(10, 0), Seq.empty, Seq(Pixel(0, 0)))
    assert(pushed == Pixel(99, 0))
    assert(FleeMineralClearance.pushClear(pushed, Seq.empty, Seq(Pixel(0, 0))) == pushed)
  }
}
