package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class ReaverTankKeepOffSuite extends AnyFunSuite {

  test("the line is absolute: inside yields, the line itself holds") {
    assert(ReaverTankKeepOff.shouldYield(12.4))
    assert( ! ReaverTankKeepOff.shouldYield(12.5))
    assert( ! ReaverTankKeepOff.shouldYield(20.0))
  }

  test("the fallback steps one tile past the line") {
    val own = Pixel(800, 500)
    val threat = Pixel(800, 500 - 10 * 32)
    val dest = ReaverTankKeepOff.fallback(own, threat)
    val legTiles = own.pixelDistance(dest) / 32.0
    assert(math.abs(legTiles - (12.5 + 1.0)) < 0.05, f"leg $legTiles%.2f != 13.5")
    assert(dest.y > own.y, "fallback must run away from the gun")
  }
}
