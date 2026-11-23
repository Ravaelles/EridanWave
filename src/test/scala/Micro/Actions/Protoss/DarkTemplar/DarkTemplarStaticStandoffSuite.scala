package Micro.Actions.Protoss.DarkTemplar

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarStaticStandoffSuite extends AnyFunSuite {

  test("pins the standoff bands") {
    assert(DarkTemplarStaticStandoff.criticalMarginTiles == 1.0)
    assert(DarkTemplarStaticStandoff.closeMarginTiles == 2.5)
    assert(DarkTemplarStaticStandoff.exitMarginTiles == 1.0)
    assert(DarkTemplarStaticStandoff.stepTiles == 0.2)
    assert(DarkTemplarStaticStandoff.neverCloserTiles == 7.4)
  }

  test("danger is the longer of gun reach and eyesight") {
    assert(DarkTemplarStaticStandoff.dangerTiles(rangeTiles = 7.0, sightTiles = 7.0) == 7.0)
    assert(DarkTemplarStaticStandoff.dangerTiles(rangeTiles = 7.0, sightTiles = 5.0) == 7.0)
    // A Cannon sees eleven tiles out while shooting seven: the band opens
    // past the eyes, never the reach, or the tile burns first.
    assert(DarkTemplarStaticStandoff.dangerTiles(rangeTiles = 7.0, sightTiles = 11.0) == 11.0)
    assert(DarkTemplarStaticStandoff.dangerTiles(rangeTiles = 4.0, sightTiles = 7.0) == 7.0)
  }

  test("a closing hull inside the critical band stops") {
    assert(DarkTemplarStaticStandoff.shouldStop(distTiles = 7.9, dangerTiles = 7.0, approaching = true, showingBack = false))
    assert(DarkTemplarStaticStandoff.shouldStop(distTiles = 8.0, dangerTiles = 7.0, approaching = true, showingBack = false))
  }

  test("a stopped, leaving or turned hull never stops") {
    assert( ! DarkTemplarStaticStandoff.shouldStop(distTiles = 7.9, dangerTiles = 7.0, approaching = false, showingBack = false))
    assert( ! DarkTemplarStaticStandoff.shouldStop(distTiles = 7.9, dangerTiles = 7.0, approaching = true, showingBack = true))
    assert( ! DarkTemplarStaticStandoff.shouldStop(distTiles = 8.1, dangerTiles = 7.0, approaching = true, showingBack = false))
  }

  test("the close band backs off, never past a stop") {
    assert(DarkTemplarStaticStandoff.shouldStep(distTiles = 9.5, dangerTiles = 7.0, approaching = true, showingBack = false))
    assert(DarkTemplarStaticStandoff.shouldStep(distTiles = 9.5, dangerTiles = 7.0, approaching = false, showingBack = false))
    assert(DarkTemplarStaticStandoff.shouldStep(distTiles = 7.9, dangerTiles = 7.0, approaching = false, showingBack = false))
    assert( ! DarkTemplarStaticStandoff.shouldStep(distTiles = 7.9, dangerTiles = 7.0, approaching = true, showingBack = false))
    assert( ! DarkTemplarStaticStandoff.shouldStep(distTiles = 9.6, dangerTiles = 7.0, approaching = false, showingBack = false))
  }

  test("the stride walks straight off the gun in 0.2-tile steps") {
    val stepped = DarkTemplarStaticStandoff.stepAway(Pixel(0, 0), Pixel(320, 0))
    assert(stepped.y == 0)
    assert(stepped.x < 0)
    assert(math.abs(Pixel(0, 0).pixelDistance(stepped) - 0.2 * 32.0) <= 1.0)
  }

  test("the latch enters sharp and releases past the margin") {
    DarkTemplarStaticStandoff.reset()
    val danger = 7.0
    val close = danger + DarkTemplarStaticStandoff.closeMarginTiles
    val release = close + DarkTemplarStaticStandoff.exitMarginTiles
    assert(DarkTemplarStaticStandoff.standoffLatched(1, close - 0.5, danger, approaching = true, showingBack = false))
    assert(DarkTemplarStaticStandoff.standoffLatched(1, close - 0.5, danger, approaching = false, showingBack = false))
    assert( ! DarkTemplarStaticStandoff.standoffLatched(1, release + 0.5, danger, approaching = false, showingBack = false))
    assert( ! DarkTemplarStaticStandoff.standoffLatched(1, close + 0.5, danger, approaching = false, showingBack = false))
  }
}
