package Micro.Corsair

import org.scalatest.funsuite.AnyFunSuite

class CorsairDanceSuite extends AnyFunSuite {

  test("a cooling Corsair far from its duel holds standoff") {
    assert(CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 12,
      distTiles     = 6.0,
      wound01       = 0.0))
  }

  test("a ready Corsair goes for the kill instead of dancing") {
    assert( ! CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 0,
      distTiles     = 6.0,
      wound01       = 0.0))
  }

  test("a close Corsair goes for the kill instead of dancing") {
    assert( ! CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 12,
      distTiles     = 2.0,
      wound01       = 0.0))
  }

  test("wounds raise the dance edge from 3 up to 5 tiles") {
    assert(CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 12,
      distTiles     = 5.1,
      wound01       = 1.0))
    assert( ! CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 12,
      distTiles     = 4.9,
      wound01       = 1.0))
    assert(CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 12,
      distTiles     = 4.9,
      wound01       = 0.0))
  }

  test("relocation is deterministic per unit and bucket") {
    assert(CorsairRelocateWhenIdle.relocateOffset(7, 3, 12) == CorsairRelocateWhenIdle.relocateOffset(7, 3, 12))
    assert(CorsairRelocateWhenIdle.relocateOffset(7, 3, 12) != CorsairRelocateWhenIdle.relocateOffset(7, 4, 12))
  }

  test("relocation offsets stay within the requested span") {
    Seq.tabulate(20)(bucket => CorsairRelocateWhenIdle.relocateOffset(7, bucket, 12)).foreach(offset => {
      assert(offset._1 >= -12 && offset._1 <= 12)
      assert(offset._2 >= -12 && offset._2 <= 12)
    })
  }
}
