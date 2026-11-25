package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class FleeMineralClearanceSuite extends AnyFunSuite {

  test("only large ground units keep clear") {
    assert(FleeMineralClearance.applies(isLarge = true, flying = false))
    assert( ! FleeMineralClearance.applies(isLarge = false, flying = false))
    assert( ! FleeMineralClearance.applies(isLarge = true, flying = true))
  }

  test("minerals keep 2.1 tiles, geysers 3.6") {
    assert(FleeMineralClearance.mineralClearTiles == 2.1)
    assert(FleeMineralClearance.geyserClearTiles == 3.6)
  }

  test("the lookup net covers the widest ring plus one") {
    assert(FleeMineralClearance.lookupRadiusTiles == 5)
    assert(FleeMineralClearance.lookupRadiusTiles * 32.0 >
      FleeMineralClearance.geyserClearTiles * 32.0)
    assert(FleeMineralClearance.lookupRadiusTiles * 32.0 >
      FleeMineralClearance.mineralClearTiles * 32.0)
  }

  test("mineral ring boundary") {
    assert( ! FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq(Pixel(67, 0)), Seq.empty))
    assert(FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq(Pixel(68, 0)), Seq.empty))
  }

  test("geyser ring boundary") {
    assert( ! FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq.empty, Seq(Pixel(115, 0))))
    assert(FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq.empty, Seq(Pixel(116, 0))))
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
    assert(pushed == Pixel(67, 0))
    assert(FleeMineralClearance.pushClear(pushed, Seq(Pixel(0, 0)), Seq.empty) == pushed)
  }

  test("the geyser push strides past it, onto the away-ray") {
    val pushed = FleeMineralClearance.pushClear(Pixel(10, 0), Seq.empty, Seq(Pixel(0, 0)))
    assert(pushed == Pixel(115, 0))
    assert(FleeMineralClearance.pushClear(pushed, Seq.empty, Seq(Pixel(0, 0))) == pushed)
  }

  test("a far-but-inside-ring geyser still vetoes - the adjacent9 trap") {
    // The old adjacent9 lookup never saw past ~1 tile, so a spot 2.2
    // tiles out passed unpushed. The kernel always vetoed it; the fix
    // is the widened net, pinned by lookupRadiusTiles above.
    assert( ! FleeMineralClearance.clearOfResources(Pixel(150, 0), Seq.empty, Seq(Pixel(80, 0))))
    val pushed = FleeMineralClearance.pushClear(Pixel(150, 0), Seq.empty, Seq(Pixel(80, 0)))
    assert(pushed == Pixel(195, 0))
    assert(FleeMineralClearance.pushClear(pushed, Seq.empty, Seq(Pixel(80, 0))) == pushed)
  }
}
