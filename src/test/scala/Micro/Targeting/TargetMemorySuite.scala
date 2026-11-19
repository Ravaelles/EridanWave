package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class TargetMemorySuite extends AnyFunSuite {

  test("a visible target never drops, contact or not") {
    assert( ! Target.shouldDropMemory(targetVisible = true, otherHittableInRange = false))
    assert( ! Target.shouldDropMemory(targetVisible = true, otherHittableInRange = true))
  }

  test("a lone memory holds until a live contact appears") {
    assert( ! Target.shouldDropMemory(targetVisible = false, otherHittableInRange = false))
  }

  test("a live contact in range releases the memory") {
    assert(Target.shouldDropMemory(targetVisible = false, otherHittableInRange = true))
  }

  test("a fogged templar memory is never legal, everything else recalls") {
    assert( ! Target.visibleHpLegal(isDarkTemplar = true, targetVisible = false))
    assert(Target.visibleHpLegal(isDarkTemplar = true, targetVisible = true))
    assert(Target.visibleHpLegal(isDarkTemplar = false, targetVisible = false))
    assert(Target.visibleHpLegal(isDarkTemplar = false, targetVisible = true))
  }

  test("a lifted building releases whoever cannot hit air") {
    assert(Target.shouldDropLifted(isFlyingBuilding = true, canHitAir = false))
    assert( ! Target.shouldDropLifted(isFlyingBuilding = true, canHitAir = true))
    assert( ! Target.shouldDropLifted(isFlyingBuilding = false, canHitAir = false))
  }
}
