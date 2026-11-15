package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class CornerAntiStuckGuardSuite extends AnyFunSuite {

  private val mapW = 4096
  private val mapH = 4096

  test("a corner destination slides out along the shallower penetration") {
    assert(CornerAntiStuckGuard.guard(Pixel(10, 10), mapW, mapH, 6.0) == Pixel(10, 192))
    assert(CornerAntiStuckGuard.guard(Pixel(4086, 10), mapW, mapH, 6.0) == Pixel(4086, 192))
    assert(CornerAntiStuckGuard.guard(Pixel(10, 4086), mapW, mapH, 6.0) == Pixel(10, mapH - 192))
  }

  test("edges and center pass through untouched") {
    assert(CornerAntiStuckGuard.guard(Pixel(2000, 10), mapW, mapH, 6.0) == Pixel(2000, 10))
    assert(CornerAntiStuckGuard.guard(Pixel(2000, 2000), mapW, mapH, 6.0) == Pixel(2000, 2000))
  }

  test("an overshooting flee clamps to the map") {
    val out = CornerAntiStuckGuard.guard(Pixel(-50, 2000), mapW, mapH, 6.0)
    assert(out == Pixel(0, 2000))
  }

  test("a custom corner width keeps clear earlier") {
    assert(CornerAntiStuckGuard.guard(Pixel(300, 300), mapW, mapH, 6.0) == Pixel(300, 300))
    assert(CornerAntiStuckGuard.guard(Pixel(300, 300), mapW, mapH, 15.0) == Pixel(300, 480))
  }

  test("the shared default keeps every air unit clear of the 10x10 squares") {
    assert(CornerAntiStuckGuard.guard(Pixel(300, 300), mapW, mapH) == Pixel(300, 320))
  }
}
