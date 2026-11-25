package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class FleeMineralClearanceSuite extends AnyFunSuite {

  test("only large ground units keep clear") {
    assert(FleeMineralClearance.applies(isLarge = true, flying = false))
    assert( ! FleeMineralClearance.applies(isLarge = false, flying = false))
    assert( ! FleeMineralClearance.applies(isLarge = true, flying = true))
  }

  test("flee spots keep 1.2 tiles off resources") {
    assert(FleeMineralClearance.resourceClearTiles == 1.2)
  }

  test("spots inside 1.2 tiles of a resource violate") {
    assert(FleeMineralClearance.violates(Pixel(0, 0), Pixel(38, 0)))
    assert( ! FleeMineralClearance.violates(Pixel(0, 0), Pixel(39, 0)))
    assert(FleeMineralClearance.violates(Pixel(0, 0), Pixel(0, 0)))
  }

  test("minerals and geysers count the same") {
    val near = Pixel(0, 30)
    val far = Pixel(500, 500)
    assert( ! FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq(near, far)))
    assert( ! FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq(far, near)))
    assert(FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq(far)))
    assert(FleeMineralClearance.clearOfResources(Pixel(0, 0), Seq.empty))
  }

  test("the push lands on the away-ray and settles") {
    val pushed = FleeMineralClearance.pushClear(Pixel(10, 0), Pixel(0, 0))
    assert(pushed.y == 0)
    assert(pushed.x >= 10)
    assert(FleeMineralClearance.pushClear(pushed, Pixel(0, 0)) == pushed)
  }
}
