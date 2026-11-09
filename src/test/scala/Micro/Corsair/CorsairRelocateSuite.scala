package Micro.Corsair

import org.scalatest.funsuite.AnyFunSuite

class CorsairRelocateSuite extends AnyFunSuite {

  test("offsets stay within the span") {
    for (id <- 0 to 50; bucket <- 0 to 5; delta <- Seq(5, 12, 18)) {
      val (dx, dy) = CorsairRelocateWhenIdle.relocateOffset(id, bucket, delta)
      assert(dx >= -delta && dx <= delta)
      assert(dy >= -delta && dy <= delta)
    }
  }

  test("the same inputs relocate to the same offset") {
    assert(CorsairRelocateWhenIdle.relocateOffset(7, 3, 12) ==
      CorsairRelocateWhenIdle.relocateOffset(7, 3, 12))
  }

  test("units spread across the span") {
    val spots = (0 to 20).map(id => CorsairRelocateWhenIdle.relocateOffset(id, 0, 12)).toSet
    assert(spots.size > 10)
  }
}
