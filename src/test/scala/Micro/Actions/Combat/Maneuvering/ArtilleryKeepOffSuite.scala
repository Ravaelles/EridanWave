package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class ArtilleryKeepOffSuite extends AnyFunSuite {

  test("a fresh hull keeps 13.1, a dying one 15.1") {
    assert(ArtilleryKeepOff.keepOffTiles(0.0) == 13.1)
    assert(ArtilleryKeepOff.keepOffTiles(1.0) == 15.1)
    assert(ArtilleryKeepOff.keepOffTiles(0.5) == 14.1)
  }

  test("the edge is sharp: inside yields, the line itself holds") {
    assert(ArtilleryKeepOff.shouldYield(13.0, 0.0))
    assert( ! ArtilleryKeepOff.shouldYield(13.1, 0.0))
    assert(ArtilleryKeepOff.shouldYield(15.0, 1.0))
    assert( ! ArtilleryKeepOff.shouldYield(15.1, 1.0))
  }

  test("the yield latches a tile past the line before the duel resumes") {
    val id = 424242
    assert(ArtilleryKeepOff.yieldLatched(id, distTiles = 13.0, wound01 = 0.0))
    assert(ArtilleryKeepOff.yieldLatched(id, distTiles = 13.5, wound01 = 0.0))
    assert( ! ArtilleryKeepOff.yieldLatched(id, distTiles = 14.2, wound01 = 0.0))
    assert( ! ArtilleryKeepOff.yieldLatched(id, distTiles = 13.5, wound01 = 0.0))
  }

  test("shells on the natural are answered on odds or five hulls, never under-gunned") {
    assert(ArtilleryKeepOff.shouldAnswer(tankAtHome = true, oursValue = 40.0, theirsValue = 100.0, combatUnitsNear = 0))
    assert(ArtilleryKeepOff.shouldAnswer(tankAtHome = true, oursValue = 400.0, theirsValue = 100.0, combatUnitsNear = 0))
    assert(ArtilleryKeepOff.shouldAnswer(tankAtHome = true, oursValue = 0.0, theirsValue = 100.0, combatUnitsNear = 5))
    assert( ! ArtilleryKeepOff.shouldAnswer(tankAtHome = true, oursValue = 39.0, theirsValue = 100.0, combatUnitsNear = 0))
    assert( ! ArtilleryKeepOff.shouldAnswer(tankAtHome = true, oursValue = 0.0, theirsValue = 100.0, combatUnitsNear = 4))
    assert( ! ArtilleryKeepOff.shouldAnswer(tankAtHome = false, oursValue = 400.0, theirsValue = 0.0, combatUnitsNear = 9))
    assert(ArtilleryKeepOff.shellsAtHome(distTankToNaturalTiles = 20.0))
    assert( ! ArtilleryKeepOff.shellsAtHome(distTankToNaturalTiles = 20.1))
  }

  test("the fallback steps one tile past the line") {    val own = Pixel(800, 500)
    val threat = Pixel(800, 500 - 10 * 32)
    val dest = ArtilleryKeepOff.fallback(own, threat, ArtilleryKeepOff.keepOffTiles(0.0))
    val legTiles = own.pixelDistance(dest) / 32.0
    assert(math.abs(legTiles - (13.1 + 1.0)) < 0.05, f"leg $legTiles%.2f != 14.1")
    assert(dest.y > own.y, "fallback must run away from the gun")
  }
}
