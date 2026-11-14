package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class FleeRelocationSuite extends AnyFunSuite {

  test("a fresh escape passes through and starts the clock") {
    FleeRelocation.reset()
    assert(FleeRelocation.relocate(7, Pixel(0, 0), Pixel(0, 320), 0) == Pixel(0, 320))
  }

  test("a stuck escape rotates ninety degrees, split by unit id") {
    FleeRelocation.reset()
    FleeRelocation.relocate(8, Pixel(0, 0), Pixel(0, 320), 0)
    val even = FleeRelocation.relocate(8, Pixel(0, 0), Pixel(0, 320), FleeRelocation.stuckFrames)
    assert(even == Pixel(-320, 0))
    FleeRelocation.reset()
    FleeRelocation.relocate(9, Pixel(0, 0), Pixel(0, 320), 0)
    val odd = FleeRelocation.relocate(9, Pixel(0, 0), Pixel(0, 320), FleeRelocation.stuckFrames)
    assert(odd == Pixel(320, 0))
  }

  test("real progress re-anchors instead of rotating") {
    FleeRelocation.reset()
    FleeRelocation.relocate(7, Pixel(0, 0), Pixel(0, 320), 0)
    assert(FleeRelocation.relocate(7, Pixel(0, 400), Pixel(0, 720), FleeRelocation.stuckFrames) == Pixel(0, 720))
  }

  test("reset clears the per-unit clock") {
    FleeRelocation.relocate(7, Pixel(0, 0), Pixel(0, 320), 0)
    FleeRelocation.reset()
    assert(FleeRelocation.relocate(7, Pixel(0, 0), Pixel(0, 320), FleeRelocation.stuckFrames) == Pixel(0, 320))
  }
}
