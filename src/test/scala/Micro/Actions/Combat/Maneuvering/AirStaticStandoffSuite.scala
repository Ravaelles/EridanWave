package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class AirStaticStandoffSuite extends AnyFunSuite {

  test("a fresh hull keeps eight, a dying one thirteen") {
    assert(AirStaticStandoff.keepOffTiles(0.0) == 8.0)
    assert(AirStaticStandoff.keepOffTiles(1.0) == 13.0)
    assert(AirStaticStandoff.keepOffTiles(0.5) == 10.5)
  }

  test("the edge is sharp: inside yields, the line itself holds") {
    assert(AirStaticStandoff.shouldYield(7.9, 0.0))
    assert( ! AirStaticStandoff.shouldYield(8.0, 0.0))
    assert(AirStaticStandoff.shouldYield(12.9, 1.0))
    assert( ! AirStaticStandoff.shouldYield(13.0, 1.0))
  }

  test("the fallback runs six tiles out with the jitter on top") {
    val own = Pixel(800, 500)
    val threat = Pixel(800, 500 - 8 * 32)
    val dest = AirStaticStandoff.fallback(own, threat, 0.0, 0.0)
    val legTiles = own.pixelDistance(dest) / 32.0
    assert(math.abs(legTiles - AirStaticStandoff.fallbackTiles) < 0.05, f"leg $legTiles%.2f != 6")

    val jittered = AirStaticStandoff.fallback(own, threat, 2 * 32.0, -2 * 32.0)
    assert(jittered.x - dest.x == 64)
    assert(jittered.y - dest.y == -64)
  }

  test("the jitter stays inside its two-tile square") {
    (1 to 200).foreach(_ => {
      val j = AirStaticStandoff.randomJitterPx()
      assert(math.abs(j) <= AirStaticStandoff.jitterTiles * 32.0, f"jitter $j%.1fpx escapes +-2 tiles")
    })
  }

  test("an injected draw pins the jitter exactly") {
    assert(AirStaticStandoff.randomJitterPx(() => 0.0) == -2.0 * 32.0)
    assert(AirStaticStandoff.randomJitterPx(() => 0.5) == 0.0)
    assert(AirStaticStandoff.randomJitterPx(() => 1.0) == 2.0 * 32.0)
  }
}
