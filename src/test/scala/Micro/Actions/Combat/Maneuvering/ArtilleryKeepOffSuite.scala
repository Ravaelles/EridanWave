package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class ArtilleryKeepOffSuite extends AnyFunSuite {

  test("a fresh hull keeps 12.8, a dying one 14.8") {
    assert(ArtilleryKeepOff.keepOffTiles(0.0) == 12.8)
    assert(ArtilleryKeepOff.keepOffTiles(1.0) == 14.8)
    assert(ArtilleryKeepOff.keepOffTiles(0.5) == 13.8)
  }

  test("the edge is sharp: inside yields, the line itself holds") {
    assert(ArtilleryKeepOff.shouldYield(12.7, 0.0))
    assert( ! ArtilleryKeepOff.shouldYield(12.8, 0.0))
    assert(ArtilleryKeepOff.shouldYield(14.7, 1.0))
    assert( ! ArtilleryKeepOff.shouldYield(14.8, 1.0))
  }

  test("the yield latches a tile past the line before the duel resumes") {
    val id = 424242
    assert(ArtilleryKeepOff.yieldLatched(id, distTiles = 12.7, wound01 = 0.0))
    assert(ArtilleryKeepOff.yieldLatched(id, distTiles = 13.5, wound01 = 0.0))
    assert( ! ArtilleryKeepOff.yieldLatched(id, distTiles = 13.9, wound01 = 0.0))
    assert( ! ArtilleryKeepOff.yieldLatched(id, distTiles = 13.5, wound01 = 0.0))
  }

  test("the fallback steps one tile past the line") {
    val own = Pixel(800, 500)
    val threat = Pixel(800, 500 - 10 * 32)
    val dest = ArtilleryKeepOff.fallback(own, threat, ArtilleryKeepOff.keepOffTiles(0.0))
    val legTiles = own.pixelDistance(dest) / 32.0
    assert(math.abs(legTiles - (12.8 + 1.0)) < 0.05, f"leg $legTiles%.2f != 13.8")
    assert(dest.y > own.y, "fallback must run away from the gun")
  }
}
