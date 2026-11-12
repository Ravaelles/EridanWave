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
}
